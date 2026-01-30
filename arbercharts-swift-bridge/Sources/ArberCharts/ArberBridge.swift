import Foundation
import CoreGraphics
#if canImport(AppKit)
import AppKit
#elseif canImport(UIKit)
import UIKit
#endif
import ArberChartsC

public final class ArberChart {
    private var isolate: OpaquePointer?
    private var thread: OpaquePointer?
    private var handle: Int64 = 0

    public init(type: Int32 = 0) {
        var iso: OpaquePointer?
        var thr: OpaquePointer?
        if graal_create_isolate(nil, &iso, &thr) == 0 {
            isolate = iso
            thread = thr
            handle = arber_create_chart(thr, type)
        }
    }

    deinit {
        if let thr = thread, handle != 0 {
            arber_destroy_chart(thr, handle)
            graal_tear_down_isolate(thr)
        }
    }

    public func isValid() -> Bool {
        return handle != 0
    }

    public func render(in context: CGContext, width: Double, height: Double) {
        guard let thr = thread, handle != 0 else { return }
        arber_set_viewport(thr, handle, 0, 0, width, height)

        let capacity = 1_048_576
        let buffer = UnsafeMutableRawPointer.allocate(byteCount: capacity, alignment: 1)
        defer { buffer.deallocate() }

        let written = arber_render_to_buffer(thr, handle, buffer.assumingMemoryBound(to: Int8.self), Int32(capacity))
        if written <= 8 { return }

        parse(buffer: buffer, length: Int(written), in: context)
    }

    private func parse(buffer: UnsafeRawPointer, length: Int, in ctx: CGContext) {
        var p = buffer.assumingMemoryBound(to: UInt8.self)

        func readU32() -> UInt32 {
            let v = UInt32(p[0]) | (UInt32(p[1]) << 8) | (UInt32(p[2]) << 16) | (UInt32(p[3]) << 24)
            p = p.advanced(by: 4)
            return v
        }

        func readF32() -> CGFloat {
            let bits = readU32()
            var f: Float = 0
            withUnsafeMutableBytes(of: &f) { b in
                b[0] = UInt8(bits & 0xFF)
                b[1] = UInt8((bits >> 8) & 0xFF)
                b[2] = UInt8((bits >> 16) & 0xFF)
                b[3] = UInt8((bits >> 24) & 0xFF)
            }
            return CGFloat(f)
        }

        func readU16() -> UInt16 {
            let v = UInt16(p[0]) | (UInt16(p[1]) << 8)
            p = p.advanced(by: 2)
            return v
        }

        if length < 8 { return }
        _ = readU32() // version
        let byteCount = Int(readU32())
        let limit = min(byteCount, length)
        let limitPtr = buffer.advanced(by: limit).assumingMemoryBound(to: UInt8.self)

        var current = CGPoint(x: 0, y: 0)
        var strokeWidth: CGFloat = 1.0
        var color = CGColor(red: 1, green: 1, blue: 1, alpha: 1)
        ctx.setStrokeColor(color)
        ctx.setFillColor(color)
        ctx.setLineWidth(strokeWidth)

        while p < limitPtr {
            let op = p.pointee
            p = p.advanced(by: 1)
            switch op {
            case 0x01:
                let argb = readU32()
                let a = CGFloat((argb >> 24) & 0xFF) / 255.0
                let r = CGFloat((argb >> 16) & 0xFF) / 255.0
                let g = CGFloat((argb >> 8) & 0xFF) / 255.0
                let b = CGFloat(argb & 0xFF) / 255.0
                color = CGColor(red: r, green: g, blue: b, alpha: a)
                ctx.setStrokeColor(color)
                ctx.setFillColor(color)
            case 0x02:
                strokeWidth = readF32()
                ctx.setLineWidth(strokeWidth)
            case 0x03:
                let x = readF32()
                let y = readF32()
                current = CGPoint(x: x, y: y)
            case 0x04:
                let x = readF32()
                let y = readF32()
                ctx.beginPath()
                ctx.move(to: current)
                ctx.addLine(to: CGPoint(x: x, y: y))
                ctx.strokePath()
                current = CGPoint(x: x, y: y)
            case 0x05:
                let count = Int(readU32())
                if count > 0 {
                    ctx.beginPath()
                    let x0 = readF32()
                    let y0 = readF32()
                    ctx.move(to: CGPoint(x: x0, y: y0))
                    if count > 1 {
                        for _ in 1..<count {
                            let x = readF32()
                            let y = readF32()
                            ctx.addLine(to: CGPoint(x: x, y: y))
                        }
                    }
                    ctx.strokePath()
                }
            case 0x06:
                let x = readF32()
                let y = readF32()
                let w = readF32()
                let h = readF32()
                ctx.stroke(CGRect(x: x, y: y, width: w, height: h))
            case 0x07:
                let x = readF32()
                let y = readF32()
                let w = readF32()
                let h = readF32()
                ctx.fill(CGRect(x: x, y: y, width: w, height: h))
            case 0x08:
                let count = Int(readU32())
                if count > 0 {
                    ctx.beginPath()
                    let x0 = readF32()
                    let y0 = readF32()
                    ctx.move(to: CGPoint(x: x0, y: y0))
                    if count > 1 {
                        for _ in 1..<count {
                            let x = readF32()
                            let y = readF32()
                            ctx.addLine(to: CGPoint(x: x, y: y))
                        }
                    }
                    ctx.closePath()
                    ctx.fillPath()
                }
            case 0x09:
                let x = readF32()
                let y = readF32()
                let w = readF32()
                let h = readF32()
                ctx.saveGState()
                ctx.clip(to: CGRect(x: x, y: y, width: w, height: h))
            case 0x0A:
                ctx.restoreGState()
            case 0x0B:
                let x = readF32()
                let y = readF32()
                let len = Int(readU16())
                let bytes = UnsafeBufferPointer(start: p, count: len)
                p = p.advanced(by: len)
                if let text = String(bytes: bytes, encoding: .utf8) {
                    #if canImport(AppKit)
                    let attrs: [NSAttributedString.Key: Any] = [
                        .foregroundColor: NSColor(cgColor: color) ?? NSColor.white
                    ]
                    (text as NSString).draw(at: CGPoint(x: x, y: y), withAttributes: attrs)
                    #elseif canImport(UIKit)
                    let attrs: [NSAttributedString.Key: Any] = [
                        .foregroundColor: UIColor(cgColor: color)
                    ]
                    (text as NSString).draw(at: CGPoint(x: x, y: y), withAttributes: attrs)
                    #else
                    _ = text
                    #endif
                }
            default:
                p = limitPtr
            }
        }
    }
}
