#pragma once

#include <QQuickItem>

extern "C" {
#include "graal_isolate.h"
}

class ArberQuickItem : public QQuickItem {
    Q_OBJECT

public:
    explicit ArberQuickItem(QQuickItem* parent = nullptr);
    ~ArberQuickItem() override;

    Q_INVOKABLE bool smokeTest();
    Q_INVOKABLE void setData(const QVector<double>& data);

protected:
    QSGNode* updatePaintNode(QSGNode* oldNode, UpdatePaintNodeData* data) override;
    void geometryChange(const QRectF& newGeometry, const QRectF& oldGeometry) override;

private:
    void ensureChart();
    int renderToBuffer(QByteArray& out);

    graal_isolate_t* isolate = nullptr;
    graal_isolatethread_t* thread = nullptr;
    long chartHandle = 0;
    QByteArray lastData;
};
