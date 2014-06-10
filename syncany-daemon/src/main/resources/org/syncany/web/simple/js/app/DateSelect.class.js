function DateSelect(sliderElements, textElements, onDatabaseVersionHeaderChange) {
	var _databaseVersionHeaders = [];
	
	var _sliderElements = sliderElements;
	var _textElements = textElements;
	var _onDatabaseVersionHeaderChange = onDatabaseVersionHeaderChange;
	
	var _dateParse = function(dateStr) {
		dateStr = dateStr.replace(/\.\d+/, "");

		// 2014-06-07 12:33:04.0 CEST
		var dateRegEx = /(\d\d\d\d)-(\d\d)-(\d\d) (\d\d):(\d\d):(\d\d)(\.(\d+))?/;
		var match = dateRegEx.exec(dateStr);
		var parsedDate = new Date(match[1], match[2]-1, match[3], match[4], match[5], match[6], 0);
		
		return moment(parsedDate).format("dd, MMM D, h:mm a");
	};
	
	var _getDateStr = function() {
		var sliderValue = _sliderElements.slider('value');
		var dateStr = _databaseVersionHeaders[sliderValue];
		
		return dateStr;
	};
	
	var _slide = function() {
		var dateStr = _getDateStr();
		var parsedDate = _dateParse(dateStr);
		
		_textElements.html(parsedDate);
	};
	
	this.updateSlider = function(databaseVersionHeaders) {
		_databaseVersionHeaders = databaseVersionHeaders;		
		
		_sliderElements.slider({
			min: 0,
			max: _databaseVersionHeaders.length-1,
			value: _databaseVersionHeaders.length-1,
			slide: _slide,
			change: function() {
				_slide();
				_onDatabaseVersionHeaderChange(_getDateStr()); // <!
			},
		});
	};
	
	this.updateSlider(_databaseVersionHeaders);
}
