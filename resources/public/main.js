;(function () {
    var buffers = {};
    var cache = {};

    var updateBuffers = function(packet) {
        var key = packet.key;
        var data = packet.data;

        if (buffers[key] === undefined) {
            buffers[key] = {"in": 0, "out": 0};
        }

        buffers[key][data.direction] += data.size;
    };


    var data = {
        labels : ["January","February","March","April","May","June","July"],
        datasets : [
            {
                fillColor : "rgba(220,220,220,0.5)",
                strokeColor : "rgba(220,220,220,1)",
                data : [65,59,90,81,56,55,40]
            },
            {
                fillColor : "rgba(151,187,205,0.5)",
                strokeColor : "rgba(151,187,205,1)",
                data : [28,48,40,19,96,27,100]
            }
        ]
    };

    var speedcalc = function() {
        _.each(buffers, function(data, host) {
            if (cache[host] === undefined) {
                cache[host] = {"in": buffers[host]["in"], "out": buffers[host]["out"]}
            } else {
                var diffIn = buffers[host]["in"] - cache[host]["in"];
                var diffOut = buffers[host]["out"] - cache[host]["out"];

                buffers[host]["speedIn"] = diffIn / 1;
                buffers[host]["speedOut"] = diffOut / 1;

                cache[host]["in"] = buffers[host]["in"];
                cache[host]["out"] = buffers[host]["out"];
            }
        });
    };

    var plotData = [];

    var draw2 = function() {
        var range = _.times(50, function() { return (new Date()).getTime(); });

        _.each(buffers, function(data, host) {
            var item = _.find(plotData, function(x) { return x.label === host; });

            if (item === undefined) {
                var zeros = _.times(50, function() { return 500; });
                item = {label: host, data: _.zip(range, zeros), rawData: zeros};
                console.log(111, item);
                plotData.push(item);
            }

            item.rawData = item.rawData.slice(1);
            item.rawData.push(buffers[host]["speedIn"] || 0)

            item.data = _.zip(range, item.rawData);
        });
    };

        Highcharts.setOptions({
                    global: {
                                    useUTC: false
                                                }
                                                        });

    $('#container').highcharts({
        chart: {
            type: 'spline',
            animation: Highcharts.svg, // don't animate in old IE
            marginRight: 10,
            events: {
                load: function() {

                    // set up the updating of the chart each second
                    var self = this;

                    setInterval(function() {
                       var x = (new Date()).getTime()
                        _.each(buffers, function(data, host) {
                            var item = _.find(self.series, function(x) { return x.name === host; });

                            if (item === undefined) {
                                var foo = {name:host, data: _.times(10, function() { return [(new Date()).getTime(), 0]; })};
                                self.addSeries(foo, true);
                                item = _.find(self.series, function(x) { return x.name === host; });
                            }

                            var y = (data["speedIn"] || 0) + (data["speedOut"] || 0);

                            console.log(item, x, y, data);
                            item.addPoint([x, y], true, true);
                        });
                    }, 1000);
                }
            }
        },
        title: {
            text: 'Live random data'
        },
        xAxis: {
            type: 'datetime',
            tickPixelInterval: 10000
        },
        yAxis: {
            tickPixelInterval: 10000,
            title: {
                text: 'Value'
            },
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
        tooltip: {
            formatter: function() {
                    return '<b>'+ this.series.name +'</b><br/>'+
                    Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) +'<br/>'+
                    Highcharts.numberFormat(this.y, 2);
            }
        },
        legend: {
            enabled: false
        },
        exporting: {
            enabled: false
        },
        series: []
        //     name: 'Random data',
        //     data: (function() {
        //         // generate an array of random data
        //         var data = [],
        //             time = (new Date()).getTime(),
        //             i;

        //         for (i = -19; i <= 0; i++) {
        //             data.push({
        //                 x: time + i * 1000,
        //                 y: Math.random()
        //             });
        //         }
        //         return data;
        //     })()
        // }]
    });

    setInterval(speedcalc, 1000);

    var source = new EventSource('/stats-sse');
    source.addEventListener('message', function(e) {
        updateBuffers(JSON.parse(e.data));
    });

    source.addEventListener('open', function(e) {
        console.log("init");
    });

    source.addEventListener('error', function(e) {
        console.log("error", e);
    });
}).call(this)
