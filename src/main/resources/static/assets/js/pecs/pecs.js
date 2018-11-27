var isDecoy;
var overviewId;
var peptideRef;
var useNoise1000;

function query(dataId, overviewId, peptideRef, cutInfo) {
    var datas = null;
    $.ajax({
        type: "POST",
        url: "/analyse/view",
        data: {
            dataId: dataId,
            overviewId: overviewId,
            peptideRef: peptideRef,
            cutInfo: cutInfo
        },
        dataType: "json",
        async: false,
        success: function (result) {
            if (result.success) {
                datas = result.model;
            } else {
                chart.clear();
            }
        }
    });

    if (datas == null) {
        return;
    }
    var data_rt = datas.rt;
    var data_intensity = datas.intensity;
    var intensity = 0;
    for (var i = 0; i < data_intensity.length; i++) {
        intensity += data_intensity[i];
    }
    option = {
        title: {
            text: peptideRef + "-总强度为:" + intensity,
            left: 10
        },
        legend: {
            data: ['rt/intensity'],
            align: 'left'
        },
        toolbox: {
            // y: 'bottom',
            feature: {
                dataView: {},
                saveAsImage: {
                    pixelRatio: 2
                }

            }
        },
        dataZoom: [{
            type: 'inside',
            filterMode: 'empty'
        }, {
            type: 'slider'
        }],
        tooltip: {
            trigger: 'axis',
            axisPointer: {            // 坐标轴指示器，坐标轴触发有效
                type: 'shadow'        // 默认为直线，可选为：'line' | 'shadow'
            }
        },
        xAxis: {
            data: data_rt,
            silent: false,
            splitLine: {
                show: false
            }
        },
        yAxis: {},
        series: [{
            name: 'intensity',
            type: 'line',
            data: data_intensity
        }]
    };

    chart.setOption(option, true);
}

function queryGroup(isDecoy, overviewId, peptideRef, isGaussFilter, useNoise1000) {
    if (isDecoy == null) {
        isDecoy = this.isDecoy;
    } else {
        this.isDecoy = isDecoy;
    }
    if (overviewId == null) {
        overviewId = this.overviewId;
    } else {
        this.overviewId = overviewId;
    }
    if (peptideRef == null) {
        peptideRef = this.peptideRef;
    } else {
        this.peptideRef = peptideRef;
    }
    if (useNoise1000 == null) {
        peptideRef = this.useNoise1000;
    } else {
        this.useNoise1000 = useNoise1000;
    }
    var datas = null;
    $.ajax({
        type: "POST",
        url: "/analyse/viewGroup",
        data: {
            isDecoy: isDecoy,
            overviewId: overviewId,
            peptideRef: peptideRef,
            isGaussFilter: isGaussFilter,
            useNoise1000: useNoise1000
        },

        dataType: "json",
        async: false,
        success: function (result) {
            if (result.success) {
                datas = result.model;
            } else {
                chartGroup.clear();
            }
        }

    });
    if (datas == null) {
        return;
    }
    var data_rt = datas.rt;
    var cutinfo = datas.cutInfoArray;
    var intensity_arrays = datas.intensityArrays;
    var bestRt = datas.bestRt;

    var intensity_series = [];
    for (var i = 0; i < intensity_arrays.length; i++) {
        intensity_series.push({
            name: cutinfo[i],
            type: 'line',
            smooth: true,
            data: intensity_arrays[i],
        });
    }

    var textLabel = peptideRef + ":" + intensity_arrays.length + "个MS2碎片;";
    if (bestRt != null) {
        textLabel = textLabel + "最佳峰RT:" + bestRt;
    }
    option = {
        title: {
            text: textLabel,
            left: 10
        },
        legend: {
            data: ['rt/intensity'],
            align: 'left'
        },
        toolbox: {
            // y: 'bottom',
            feature: {
                dataView: {},
                saveAsImage: {
                    pixelRatio: 2
                }

            }
        },
        dataZoom: [{
            type: 'inside',
            filterMode: 'empty',
            start: (chartGroup.getModel() && chartGroup.getModel().getOption().dataZoom[0].start) ? chartGroup.getModel().getOption().dataZoom[0].start : 0,
            end: (chartGroup.getModel() && chartGroup.getModel().getOption().dataZoom[0].end) ? chartGroup.getModel().getOption().dataZoom[0].end : 100
        }, {
            type: 'slider'
        }],
        tooltip: {
            trigger: 'axis',
            axisPointer: {            // 坐标轴指示器，坐标轴触发有效
                type: 'shadow'        // 默认为直线，可选为：'line' | 'shadow'
            }
        },
        xAxis: {
            data: data_rt,
            silent: false,
            splitLine: {
                show: false
            }
        },
        yAxis: {},
        series: intensity_series
    };

    chartGroup.setOption(option, true);

}