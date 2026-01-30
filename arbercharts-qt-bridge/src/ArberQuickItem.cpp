#include "ArberQuickItem.h"
#include <QColor>
#include <QPainter>
#include <QSGSimpleTextureNode>
#include <QQuickItem>
#include <QQuickWindow>

extern "C" {
#include "arbercharts-core.h"
}

ArberQuickItem::ArberQuickItem(QQuickItem* parent)
    : QQuickItem(parent) {
    this->setFlag(QQuickItem::ItemHasContents, true);
}

ArberQuickItem::~ArberQuickItem() {
    if (thread && chartHandle != 0) {
        arber_destroy_chart(thread, chartHandle);
        graal_tear_down_isolate(thread);
    }
}

bool ArberQuickItem::smokeTest() {
    ensureChart();
    return chartHandle != 0;
}

void ArberQuickItem::setData(const QVector<double>& data) {
    lastData.clear();
    lastData.reserve(static_cast<int>(data.size() * sizeof(double)));
    for (double v : data) {
        const char* p = reinterpret_cast<const char*>(&v);
        lastData.append(p, static_cast<int>(sizeof(double)));
    }
    this->update();
}

QSGNode* ArberQuickItem::updatePaintNode(QSGNode* oldNode, UpdatePaintNodeData* data) {
    Q_UNUSED(data)
    ensureChart();
    QSGSimpleTextureNode* node = static_cast<QSGSimpleTextureNode*>(oldNode);
    if (!node) {
        node = new QSGSimpleTextureNode();
    }

    const int w = static_cast<int>(this->width());
    const int h = static_cast<int>(this->height());
    if (w <= 0 || h <= 0) {
        return node;
    }

    if (!lastData.isEmpty()) {
        const double* values = reinterpret_cast<const double*>(lastData.constData());
        int count = static_cast<int>(lastData.size() / sizeof(double));
        arber_update_data(thread, chartHandle, const_cast<double*>(values), count);
    }

    QByteArray buffer;
    int size = renderToBuffer(buffer);
    if (size <= 0) {
        return node;
    }

    QImage image(w, h, QImage::Format_ARGB32_Premultiplied);
    image.fill(Qt::transparent);
    QPainter painter(&image);
    painter.setRenderHint(QPainter::Antialiasing, true);

    const unsigned char* ptr = reinterpret_cast<const unsigned char*>(buffer.constData());
    const unsigned char* end = ptr + size;

    auto readU32 = [&](const unsigned char*& p) -> quint32 {
        quint32 v = p[0] | (p[1] << 8) | (p[2] << 16) | (p[3] << 24);
        p += 4;
        return v;
    };
    auto readF32 = [&](const unsigned char*& p) -> float {
        quint32 bits = readU32(p);
        float f;
        std::memcpy(&f, &bits, sizeof(float));
        return f;
    };
    auto readU16 = [&](const unsigned char*& p) -> quint16 {
        quint16 v = p[0] | (p[1] << 8);
        p += 2;
        return v;
    };

    if (end - ptr < 8) {
        return node;
    }
    quint32 version = readU32(ptr);
    Q_UNUSED(version)
    quint32 byteCount = readU32(ptr);
    if (byteCount > buffer.size()) {
        byteCount = buffer.size();
    }
    end = reinterpret_cast<const unsigned char*>(buffer.constData()) + byteCount;

    QPointF current(0, 0);
    QColor color = Qt::white;
    float stroke = 1.0f;
    QPen pen(color);
    pen.setWidthF(stroke);
    painter.setPen(pen);
    painter.setBrush(color);

    while (ptr < end) {
        quint8 op = *ptr++;
        switch (op) {
            case 0x01: {
                quint32 argb = readU32(ptr);
                color = QColor::fromRgba(argb);
                pen.setColor(color);
                painter.setPen(pen);
                painter.setBrush(color);
                break;
            }
            case 0x02: {
                stroke = readF32(ptr);
                pen.setWidthF(stroke);
                painter.setPen(pen);
                break;
            }
            case 0x03: {
                float x = readF32(ptr);
                float y = readF32(ptr);
                current = QPointF(x, y);
                break;
            }
            case 0x04: {
                float x = readF32(ptr);
                float y = readF32(ptr);
                painter.drawLine(current, QPointF(x, y));
                current = QPointF(x, y);
                break;
            }
            case 0x05: {
                quint32 count = readU32(ptr);
                QPolygonF poly;
                poly.reserve(static_cast<int>(count));
                for (quint32 i = 0; i < count; i++) {
                    float x = readF32(ptr);
                    float y = readF32(ptr);
                    poly.append(QPointF(x, y));
                }
                painter.drawPolyline(poly);
                break;
            }
            case 0x06: {
                float x = readF32(ptr);
                float y = readF32(ptr);
                float w = readF32(ptr);
                float h = readF32(ptr);
                painter.drawRect(QRectF(x, y, w, h));
                break;
            }
            case 0x07: {
                float x = readF32(ptr);
                float y = readF32(ptr);
                float w = readF32(ptr);
                float h = readF32(ptr);
                painter.fillRect(QRectF(x, y, w, h), painter.brush());
                break;
            }
            case 0x08: {
                quint32 count = readU32(ptr);
                QPolygonF poly;
                poly.reserve(static_cast<int>(count));
                for (quint32 i = 0; i < count; i++) {
                    float x = readF32(ptr);
                    float y = readF32(ptr);
                    poly.append(QPointF(x, y));
                }
                painter.drawPolygon(poly);
                break;
            }
            case 0x09: {
                float x = readF32(ptr);
                float y = readF32(ptr);
                float w = readF32(ptr);
                float h = readF32(ptr);
                painter.save();
                painter.setClipRect(QRectF(x, y, w, h));
                break;
            }
            case 0x0A: {
                painter.restore();
                break;
            }
            case 0x0B: {
                float x = readF32(ptr);
                float y = readF32(ptr);
                quint16 len = readU16(ptr);
                QByteArray text(reinterpret_cast<const char*>(ptr), len);
                ptr += len;
                painter.drawText(QPointF(x, y), QString::fromUtf8(text));
                break;
            }
            default:
                ptr = end;
                break;
        }
    }

    painter.end();

    QSGTexture* texture = this->window()->createTextureFromImage(image);
    node->setTexture(texture);
    node->setRect(this->boundingRect());
    return node;
}

void ArberQuickItem::geometryChange(const QRectF& newGeometry, const QRectF& oldGeometry) {
    QQuickItem::geometryChange(newGeometry, oldGeometry);
    if (newGeometry.size() == oldGeometry.size()) {
        return;
    }
    const int w = static_cast<int>(newGeometry.width());
    const int h = static_cast<int>(newGeometry.height());
    if (w <= 0 || h <= 0) {
        return;
    }
    ensureChart();
    if (thread && chartHandle != 0) {
        arber_set_viewport(thread, chartHandle, 0.0, 0.0, w, h);
    }
    this->update();
}

void ArberQuickItem::ensureChart() {
    if (thread && chartHandle != 0) return;
    if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
        isolate = nullptr;
        thread = nullptr;
        chartHandle = 0;
        return;
    }
    chartHandle = arber_create_chart(thread, 0);
}

int ArberQuickItem::renderToBuffer(QByteArray& out) {
    if (!thread || chartHandle == 0) return -1;
    const int w = static_cast<int>(this->width());
    const int h = static_cast<int>(this->height());
    arber_set_viewport(thread, chartHandle, 0.0, 0.0, w, h);

    int capacity = 1024 * 1024;
    out.resize(capacity);
    int written = arber_render_to_buffer(thread, chartHandle, reinterpret_cast<char*>(out.data()), capacity);
    if (written > capacity) {
        out.resize(written);
        written = arber_render_to_buffer(thread, chartHandle, reinterpret_cast<char*>(out.data()), written);
    }
    return written;
}
