 $(function () {
 	$("#dialog-connect").dialog({
 		autoOpen: false,
 		height: 300,
 		width: 350,
 		modal: true,
 		buttons: {
 			"Connect": function () {
 				/*var bValid = true;
 				allFields.removeClass("ui-state-error");
 				bValid = bValid && checkLength(name, "username", 3, 16);
 				bValid = bValid && checkLength(email, "email", 6, 80);
 				bValid = bValid && checkLength(password, "password", 5, 16);
 				bValid = bValid && checkRegexp(name, /^[a-z]([0-9a-z_])+$/i, "Username may consist of a-z, 0-9, underscores, begin with a letter.");
 				// From jquery.validate.js (by joern), contributed by Scott Gonzalez: http://projects.scottsplayground.com/email_address_validation/
 				bValid = bValid && checkRegexp(email, /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i, "eg. ui@jquery.com");
 				bValid = bValid && checkRegexp(password, /^([0-9a-zA-Z])+$/, "Password field only allow : a-z 0-9");
 				if (bValid) {
 					$("#users tbody").append("<tr>" +
 						"<td>" + name.val() + "</td>" +
 						"<td>" + email.val() + "</td>" +
 						"<td>" + password.val() + "</td>" +
 						"</tr>");
 					$(this).dialog("close");
 				}*/
 				
 				$(this).dialog("close");
 			},
 			"Cancel": function () {
 				$(this).dialog("close");
 			}
 		},
 		close: function () {
 			//allFields.val("").removeClass("ui-state-error");
 		}
 	});
 	
 	
	$("#button-connect").button({
		icons: {
			primary: "ui-icon-locked"
		},
		text: false
	})
	.click(function() {
		$("#dialog-connect").dialog("open");
	});
	
 });
