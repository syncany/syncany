function Table(tableElements, onContextFileInfoClick, onContextPreviousVersionsClick) {
	var dataTable = null;

	this.populateTable = function(prefix, fileVersions) {
		tableElements.dataTable().fnDestroy();
	
		dataTable = $('#table').DataTable({
			paging: false,
			searching: false,
			jQueryUI: false,
			info: false,
			ordering: true,
			data: fileVersions,
			columns: [
				{ data: 'path', className: 'path', /*, type: "path", orderDataType: "dom-text"*/render: function(path) { 
					return basename(path); 
				}},
				{ data: 'version' },
				{ data: 'type', visible: false },
				{ data: 'status', visible: false },
				{ data: 'size', className: "right", render: function(bytes) { 
					return "<span title='" + bytes + " byte(s)'>" + formatFileSize(bytes) + "</span>";
				}},
				{ data: 'lastModified' },
				{ data: 'posixPermissions' },
			],
			createdRow: function (row, data, index) {
				if (data.type == "FOLDER") {
					$('td', row).eq(0).prepend("<span class='folder'></span>");
				}
				else {
					$('td', row).eq(0).prepend("<span class='file'></span>");
				}
			}
		});
		/*
		jQuery.fn.dataTableExt.afnSortData['dom-text'] = function  ( oSettings, iColumn )
		{
			console.log(oSettings);
			console.log(iColumn);
		    var aData = [];
		    $( 'td:eq('+iColumn+') input', oSettings.oApi._fnGetRowElements(oSettings, iColumn) ).each( function () {
			aData.push( this.value );
		    } );
		    return aData;
		}
		*/	
		/*
		var pathSort = function(a, b, ascDesc) {
			console.log("sort");
			console.log(typeof a);
			var aType = a.type == 'file' || a.type == 'symlink' ? 'file' : 'folder';
			var bType = b.type == 'file' || b.type == 'symlink' ? 'file' : 'folder';

			if (aType == bType) {
				return a.path.compare(b.path);
			}
			else {
				return aType.compare(bType);
			}
		};*/
		/*
		$.fn.dataTableExt.oSort['path-asc'] = function(a, b, c) {
			return pathSort(a, b, "asc");
		};

		$.fn.dataTableExt.oSort['path-desc'] = function(a, b) {
			return pathSort(a, b, "desc");
		};
		*/
		dataTable.on('mouseenter', function (ctx) {       
			$(this).contextMenu({
				selector: 'tr', 
				build: function(row, e) {
					var file = dataTable.row(row).data();
					var items = {};
				
					if (file.type.toLowerCase() == 'file' || file.type.toLowerCase() == 'symlink') {
						var filename = basename(file.path);
						var restoreDisabled = file.version == 1;
					
						items = {
							"preview": {name: "Preview " + filename, icon: "cut"},
							"download": {name: "Download " + filename, icon: "cut"},
							"show-previous": {name: "Previous versions", icon: "edit", disabled: restoreDisabled},
							"sep1": "---------",
							"show-details": {name: "File details", icon: "edit"},
						};
					}
					else {
						items = {
							"show-details": {name: "File details", icon: "edit"},
						}
					}				
				
					return {
						callback: function(action, options) {
							var file = dataTable.row(this).data();
				
							if (action == "show-details") {
								onContextFileInfoClick(file);
							}
							else if (action == "show-previous") {
								onContextPreviousVersionsClick(file);
							}
						},
						items: items
					}
				}
			});
		});
	
		tableElements.$('tbody tr').click(function () {
			var file = dataTable.row(this).data();

			if (file.type.toLowerCase() == "file") {
				// Highlight
				if ($(this).hasClass('selected')) {
					$(this).removeClass('selected');
				}
				else {
					dataTable.$('tr.selected').removeClass('selected');
					$(this).addClass('selected');
				}
		
				// Download
				retrieveFile(file);
			}
			else {
				sendFileTreeRequest(file.path+"/");
			}
		});

	};
}
