(function(document, $) {
	"use strict";

	$(document).on("foundation-contentloaded", function() {
		var bouton = document.getElementById('bouton');
		if (bouton != null) {
			bouton.style.display = 'none';
		}
	});

	$(document).on("change", function() {
		oc();
	});

	function oc() {
		var habillage = document.getElementById('habillage');
		var flyer = document.getElementById('flyer');

		if (habillage != null & flyer != null) {

			if (habillage.checked == true) {
				document.getElementById('bouton').style.display = 'none';
			} else if (flyer.checked == true) {
				document.getElementById('bouton').style.display = 'block';
			}
		}
	}
})(document, Granite.$);