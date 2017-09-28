function resourceChart(id, cpuSeries, ramSeries, playersOnlineSeries) {
	Highcharts.stockChart(id, {
		rangeSelector: {
			selected: 1,
			buttons: [{
				type: 'hour',
				count: 12,
				text: '12h'
			},{
				type: 'hour',
				count: 24,
				text: '24h'
			},{
				type: 'day',
				count: 7,
				text: '7d'
			},{
				type: 'month',
				count: 1,
				text: '30d'
			},{
				type: 'all',
				text: 'All'
			}]
		},
		tooltip: {
			split: true
		},
		title: {text: ''},
		plotOptions: {
			areaspline: {
				fillOpacity: 0.4
			}
		},
		legend: {
            enabled: true,
        },
		series: [cpuSeries, ramSeries, playersOnlineSeries]
	});
}