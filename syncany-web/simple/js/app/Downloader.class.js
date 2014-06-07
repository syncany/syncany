function Downloader() {
	this._init = function() {
		this.frames = [];
		this.framesExpected = 0;
		this.mimeType = "";
		this.filename = "";	
		this.fileLength = 0;	
	}
	
	this.setMetaData = function(fileName, fileLength, mimeType, framesExpected) {
		this.fileName = fileName;
		this.fileLength = fileLength;
		this.mimeType = mimeType;
		this.framesExpected = framesExpected;
	};	
	
	this.processBlobMessage = function(blob) {
		this.frames[this.frames.length] = blob;

		if (this.frames.length == this.framesExpected) {
			this._deliverFile();
		}
	};
	
	this._deliverFile = function() {
		var fullBlob = new Blob(this.frames, { type: this.mimeType });
		
		if (this.mimeType.indexOf('image/') >= 0) {
			$('#preview').html("<img src='' />");
			
			var fileReader = new FileReader();
			
			fileReader.onloadend = function () {
				$('#preview img')[0].src = fileReader.result;
			}
			
			fileReader.readAsDataURL(fullBlob);
		}
		else if (this.mimeType.indexOf('text/') >= 0) {
			$('#preview').html("<textarea readonly='readonly'></textarea>");
			
			var fileReader = new FileReader();
			
			fileReader.onloadend = function () {
				$('#preview textarea').text(fileReader.result);
			}
			
			fileReader.readAsText(fullBlob);
		}
		else {
			saveAs(fullBlob, this.fileName);
		}

		this._init();
	};
	
	this._init();
};
