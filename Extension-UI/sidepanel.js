/**
 * Buffer Extension Sidepanel Controller
 * Manages sidepanel UI and coordinates content extraction for AI brainstorm functionality
 */

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
		showBrainstormView();
	});

	$("#back-button").click(function() {
		showMainView();
	});
	
	/**
	 * Opens Buffer's post composer in a new tab
	 */
	function openPostComposer(tabId) {
		const message = {
		  data: {
			tabId: tabId,
		  },
		  action: "PUBLISHTAB"
		}
		
		chrome.runtime.sendMessage(message, function() {
			// Side panel stays open
		});
	}
	
	/**
	 * Opens Buffer's idea composer in a new tab
	 */
	function openIdeaComposer(tabId) {
		const message = {
		  data: {
			tabId: tabId,
		  },
		  action: "IDEASTAB"
		}
		
		chrome.runtime.sendMessage(message, function() {
			// Side panel stays open
		});
	}

	/**
	 * Shows the AI brainstorm interface and initiates page content extraction
	 */
	function showBrainstormView() {
		$("#main-view").hide();
		$("#brainstorm-view").show();
		
		$("#brainstorm-root").html(`
			<div style="
				display: flex; 
				flex-direction: column;
				align-items: center; 
				justify-content: center; 
				height: 100%; 
				font-family: 'Roboto', sans-serif;
				color: #2c4bff;
			">
				<div style="font-size: 18px; font-weight: 500; margin-bottom: 20px;">
					Buffer-ing<span class="loading-dots">.....</span>
				</div>
				<div style="font-size: 12px; color: #666;">
					Extracting page content...
				</div>
			</div>
			<style>
				@keyframes bounce {
					0%, 80%, 100% { transform: translateY(0); opacity: 0.3; }
					40% { transform: translateY(-10px); opacity: 1; }
				}
				.loading-dots span {
					animation: bounce 1.4s infinite ease-in-out;
					animation-fill-mode: both;
					display: inline-block;
				}
				.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
				.loading-dots span:nth-child(2) { animation-delay: -0.16s; }
				.loading-dots span:nth-child(3) { animation-delay: 0s; }
				.loading-dots span:nth-child(4) { animation-delay: 0.16s; }
				.loading-dots span:nth-child(5) { animation-delay: 0.32s; }
			</style>
		`);
		
		setTimeout(() => {
			const dotsContainer = $(".loading-dots");
			if (dotsContainer.length) {
				dotsContainer.html('<span>.</span><span>.</span><span>.</span><span>.</span><span>.</span>');
			}
		}, 100);
		
		chrome.tabs.query({ active: true, currentWindow: true }, function(tabs) {
			const tab = tabs[0];
			const requestId = Date.now().toString();
			
			const message = {
				action: "GET_PAGE_CONTENT",
				data: {
					tabId: tab.id,
					requestId: requestId
				}
			};
			
			const messageListener = function(response) {
				if (response.action === "PAGE_CONTENT_RESULT" && response.requestId === requestId) {
					chrome.runtime.onMessage.removeListener(messageListener);
					
					const storageKeys = [`pageContent_${requestId}`];
					if (response.hasCachedApiResponse) {
						storageKeys.push(`apiResponse_${requestId}`);
					}
					
					chrome.storage.local.get(storageKeys, function(result) {
						const content = result[`pageContent_${requestId}`];
						const cachedApiResponse = result[`apiResponse_${requestId}`] || null;
						
						chrome.storage.local.remove(storageKeys);
						
						initializeBrainstormWithContent(content || {
							error: 'No content found in storage',
							fullText: '',
							title: tab.title || '',
							url: tab.url || ''
						}, cachedApiResponse);
					});
				}
			};
			
			chrome.runtime.onMessage.addListener(messageListener);
			
			chrome.runtime.sendMessage(message, function(response) {
				if (chrome.runtime.lastError) {
					console.error('Error sending message:', chrome.runtime.lastError);
				}
			});
			
			// Fallback timeout
			setTimeout(() => {
				chrome.runtime.onMessage.removeListener(messageListener);
				initializeBrainstormWithContent({
					fullText: '',
					title: tab.title || '',
					url: tab.url || '',
					description: '',
					headings: [],
					extractedAt: new Date().toISOString()
				}, null);
			}, 6000);
		});
	}

	/**
	 * Bridge function between Chrome extension and React components
	 * Initializes the React brainstorm interface with extracted content
	 */
	function initializeBrainstormWithContent(pageContent, cachedApiResponse = null) {
		if (!window.brainstormInitialized) {
			if (typeof window.initializeBrainstormFlow === 'function') {
				window.initializeBrainstormFlow(pageContent, cachedApiResponse);
				window.brainstormInitialized = true;
			} else {
				setTimeout(function() {
					if (typeof window.initializeBrainstormFlow === 'function') {
						window.initializeBrainstormFlow(pageContent, cachedApiResponse);
						window.brainstormInitialized = true;
					}
				}, 100);
			}
		}
	}

	/**
	 * Returns to the main menu view from brainstorm interface
	 */
	function showMainView() {
		$("#brainstorm-view").hide();
		$("#main-view").show();
		
		// Reset the brainstorm initialization flag to allow fresh initialization
		window.brainstormInitialized = false;
		
		// Clear any cached API response for the current tab
		chrome.tabs.query({ active: true, currentWindow: true }, function(tabs) {
			if (tabs[0]) {
				chrome.runtime.sendMessage({
					action: "CLEAR_CACHE_FOR_TAB",
					data: {
						tabId: tabs[0].id,
						url: tabs[0].url
					}
				});
			}
		});
	}

	// Fallback function if React bundle doesn't load
	if (typeof window.initializeBrainstormFlow === 'undefined') {
		window.initializeBrainstormFlow = function() {
			// Fallback - React bundle not loaded
		};
	}
}); 