#if canImport(SwiftUI)
import SwiftUI

public struct ArberChartView: View {
    private let chart: ArberChart

    public init(chart: ArberChart = ArberChart()) {
        self.chart = chart
    }

    public var body: some View {
        GeometryReader { geo in
            Canvas { context, _ in
                let size = geo.size
                context.withCGContext { cg in
                    chart.render(in: cg, width: Double(size.width), height: Double(size.height))
                }
            }
        }
    }
}
#endif
