/* globals key */

// requires keymaster.js

;(function() { 
  // Wait for xt.options to be set
  ;(function check() {
    // If hotkey is switched on, add the buttons
    chrome.storage.local.get(['buffer.op.key-enable', 'buffer.op.key-combo'], function(items) {
      if (items['buffer.op.key-enable'] === 'key-enable') {
        if(!window.hotKeyLoaded){
          key(items['buffer.op.key-combo'], function() {
            const message = { data: { "text": document.title, "url": window.location.href }, action: "PUBLISH" }
            chrome.runtime.sendMessage(message);
            return false;
          });
          window.hotKeyLoaded = true;
        }
      } else {
        setTimeout(check, 2000);
      }
    });
  }());
}());