$(document).ready(function() {
	$("#create-post").click(function() {
		chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
			var tab = tabs[0];
			openPostComposer(tab.id);
		});
	});

	$("#create-idea").click(function() {
		chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
			let tab = tabs[0];
			openIdeaComposer(tab.id);
		});
	});

	$("#brainstorm-ai").click(function() {
		chrome.tabs.query({ 'active': true, 'currentWindow': true }, function(tabs) {
			let tab = tabs[0];
			openAIBrainstorm(tab.id);
		});
	});
	
	function openPostComposer(tabId) {
		const message = {
		  data: {
			tabId: tabId,
		  },
		  action: "PUBLISHTAB"
		}
		
		chrome.runtime.sendMessage(message, function() {
			// Side panel stays open, so we don't need to close anything
		});
	}
	
	function openIdeaComposer(tabId) {
		const message = {
		  data: {
			tabId: tabId,
		  },
		  action: "IDEASTAB"
		}
		
		chrome.runtime.sendMessage(message, function() {
			// Side panel stays open, so we don't need to close anything
		});
	}

	function openAIBrainstorm(tabId) {
		const message = {
		  data: {
			tabId: tabId,
		  },
		  action: "AIBRAINSTORM"
		}
		
		chrome.runtime.sendMessage(message, function() {
			// Side panel stays open, so we don't need to close anything
		});
	}
}); 