function Tree(treeElements, onFileClick) {
	this._init = function() {
		treeElements.jstree({
			core: {
				data: function (obj, cb) {
				    cb.call(this, []);
				},
				animation: false,
				check_callback : true,
				themes: {
					stripes: true,
					responsive: false
				}
			},
			sort: function(a, b) {
				return this.get_type(a) === this.get_type(b) ? (this.get_text(a) > this.get_text(b) ? 1 : -1) : (this.get_type(a) >= this.get_type(b) ? -1 : 1);
			},
			types: {
				'up' : { 'icon' : 'jstree-folder' },
				'folder' : { 'valid_children': ['folder'], 'icon' : 'jstree-folder' },
				'file' : { 'valid_children' : [], 'icon' : 'jstree-file' }
			},
			plugins: ['sort', 'types', 'wholerow'] 

		})
		.on("activate_node.jstree", function (e, data) {
			if (data.node.original) {
				onFileClick(data.node.original.file);
			}
			else {
				onFileClick(false);
			}
	 	})
	 	.on("load_node.jstree", function (e, data) {
			if (data.node.original) {
				onFileClick(data.node.original.file);
			}
			else {
				onFileClick(false);
			}
	 	});
	 	
	 	this.tree = $.jstree.reference('#'+treeElements[0].id);
	}
	
	this.populateTree = function(prefix, fileVersions) {
		var tree = this.tree;
		var parentNode = (prefix != "") ? tree.get_node(prefix.substr(0, prefix.length-1)) : "ROOT";

		console.log("parent node");
		console.log(prefix.substr(0, prefix.length-1));

		$(fileVersions).each(function (i, file) {	
			var newNodeId = file.path;
			var newNode = tree.get_node(newNodeId);

			if (!newNode) {
				if (file.type.toLowerCase() == "folder") {
					var newNodeText = basename(file.path);
				
					tree.create_node(parentNode, {
						id: newNodeId,
						text: newNodeText,
						type: file.type.toLowerCase(),
						file: file,
						children: true
					});
				}
			}
		});
		
		tree.deselect_all();
		tree.open_node(parentNode);
		tree.select_node(parentNode);
		
		tree.scrollTop = 0;
	}
	
	this.clear = function(root) {
		// Clear tree entries
		var i=0;
		while (i++<1000) {
			var node = treeElements.find('li');
	
			if (!node) {
				break;
			}
			else {
				this.tree.delete_node(node);
			}
		}
		
		// Create root node
		var rootNode = this.tree.create_node(null, {
			id: "ROOT",
			text: basename(root),
			type: "folder",
			children: true
		});
		
		// Select root node
		this.tree.select_node(rootNode);
	}
	
	this._init();
}


