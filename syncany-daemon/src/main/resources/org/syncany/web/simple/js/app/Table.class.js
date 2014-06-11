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
				{ data: 'path', className: 'path', orderData: [ 2, 0 ], render: function(path) { 
					return basename(path); 
				}},
				{ data: 'version' },
				{ data: 'sortType', visible: false },
				{ data: 'status', visible: false },
				{ data: 'size', className: "right", render: function(bytes) { 
					return "<span title='" + bytes + " byte(s)'>" + formatFileSize(bytes) + "</span>";
				}},
				{ data: 'lastModified' },
				{ data: 'posixPermissions' },
//				{ data: 'dosAttributes' }
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
