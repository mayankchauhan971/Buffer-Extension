/**
 * Buffer Extension Background Script
 * Manages extension lifecycle, content extraction, and Chrome API integration
 */

/**
 * LRU Cache for Page Content
 * Caches extracted page content to avoid re-processing on repeated requests
 */
class ContentCache {
  constructor(maxSize = 5, expirationMs = 15 * 60 * 1000) {
    this.maxSize = maxSize;
    this.expirationMs = expirationMs;
    this.cache = new Map();
  }

  _generateKey(tabId, url) {
    return `${tabId}_${url}`;
  }

  _isValidEntry(entry) {
    return (Date.now() - entry.timestamp) <= this.expirationMs;
  }

  get(tabId, url) {
    const key = this._generateKey(tabId, url);
    const entry = this.cache.get(key);
    
    if (!entry || !this._isValidEntry(entry)) {
      if (entry) this.cache.delete(key);
      return null;
    }

    // Move to end (LRU)
    this.cache.delete(key);
    this.cache.set(key, entry);
    return entry.content;
  }

  getFullEntry(tabId, url) {
    const key = this._generateKey(tabId, url);
    const entry = this.cache.get(key);
    
    if (!entry || !this._isValidEntry(entry)) {
      if (entry) this.cache.delete(key);
      return null;
    }

    // Move to end (LRU)
    this.cache.delete(key);
    this.cache.set(key, entry);
    return entry;
  }

  set(tabId, url, content, apiResponse = null) {
    const key = this._generateKey(tabId, url);
    
    // Evict oldest entry if at capacity
    if (this.cache.size >= this.maxSize) {
      const oldestKey = this.cache.keys().next().value;
      this.cache.delete(oldestKey);
    }

    this.cache.set(key, {
      content,
      apiResponse,
      timestamp: Date.now()
    });
  }

  setApiResponse(tabId, url, apiResponse) {
    const key = this._generateKey(tabId, url);
    const entry = this.cache.get(key);
    
    if (entry && this._isValidEntry(entry)) {
      entry.apiResponse = apiResponse;
      this.cache.delete(key);
      this.cache.set(key, entry);
    }
  }

  clear() {
    this.cache.clear();
  }

  clearForTab(tabId, url) {
    const key = this._generateKey(tabId, url);
    this.cache.delete(key);
  }
}

const contentCache = new ContentCache();

init();

function init() {
  var action = chrome.action != null ? chrome.action : chrome.browserAction;
  action.onClicked.addListener((tab) => {
    chrome.sidePanel.open({ windowId: tab.windowId });
  });

  setDefaultSettings();
  setContextMenus();
  setIcon();
  setUninstallPage();
}

function setUninstallPage() {
  chrome.runtime.setUninstallURL("https://support.buffer.com");
}

chrome.runtime.onInstalled.addListener(function (details) {
  if (details.reason === chrome.runtime.OnInstalledReason.INSTALL) {
    chrome.tabs.create({
      url: "https://support.buffer.com/article/653-buffer-browser-extension",
    });
  } else if (details.reason === chrome.runtime.OnInstalledReason.UPDATE) {
    chrome.storage.local.get("migrated", function (result) {
      if (JSON.stringify(result) === "{}") {
        chrome.windows.getCurrent({}, function (currentWindow) {
          var popupWidth = Math.min(740, currentWindow.width);
          var popupHeight = Math.min(700, currentWindow.height);
          var popupLeft = Math.round((currentWindow.width - popupWidth) / 2);
          var popupTop = Math.round((currentWindow.height - popupHeight) / 2);

          chrome.windows.create({
            url: chrome.runtime.getURL("migration.html"),
            type: "popup",
            width: popupWidth,
            height: popupHeight,
            top: popupTop,
            left: popupLeft,
          });
        });
      } else {
        setDefaultSettings();
      }
    });
  }

  init();
});

// Keyboard shortcuts
if (chrome.commands) {
  chrome.commands.onCommand.addListener((command, tab) => {
    if (command === "share-to-buffer-action") openPublishWithTab(tab.id);
  });
}

/**
 * Creates context menu items for different content types
 */
function setContextMenus() {
  chrome.contextMenus.removeAll();

  // Page context menus
  chrome.contextMenus.create({
    title: "Create Post",
    id: "pageContextPublish",
    contexts: ["page"],
  });

  chrome.contextMenus.create({
    title: "Save Idea",
    id: "pageContextIdeas",
    contexts: ["page"],
  });

  chrome.contextMenus.create({
    title: "Brainstorm with AI",
    id: "pageContextAI",
    contexts: ["page"],
  });

  // Selection context menus
  chrome.contextMenus.create({
    title: "Create Post",
    id: "selectionContextPublish",
    contexts: ["selection"],
  });

  chrome.contextMenus.create({
    title: "Save Idea",
    id: "selectionContextIdeas",
    contexts: ["selection"],
  });

  chrome.contextMenus.create({
    title: "Brainstorm with AI",
    id: "selectionContextAI",
    contexts: ["selection"],
  });

  chrome.contextMenus.create({
    type: "separator",
    id: "selectionDivider",
    contexts: ["selection"],
  });
  
  // Image context menus
  chrome.contextMenus.create({
    title: "Create Post",
    id: "imageContextPublish",
    contexts: ["image"],
  });

  chrome.contextMenus.create({
    title: "Save Idea",
    id: "imageContextIdeas",
    contexts: ["image"],
  });

  chrome.contextMenus.create({
    title: "Brainstorm with AI",
    id: "imageContextAI",
    contexts: ["image"],
  });
}

/**
 * Opens Buffer's post composer with current tab content
 */
async function openPublishWithTab(tabId) {
  const title = await getTabContent(tabId);

  chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
    var tab = tabs[0];
    let url = tab.url;

    const data = {
      text: title,
      url: url,
      placement: "hover_button_image",
    };

    var publishURL = new URL("https://publish.buffer.com/compose");
    publishURL.search = new URLSearchParams(data);
    chrome.tabs.create({
      url:
        "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
        encodeURIComponent(publishURL.toString()),
    });
  });
}

/**
 * Opens Buffer's post composer with selected text
 */
function openPublishWithSelection(info) {
  let composeRoute =
    "https://publish.buffer.com/compose?text=" +
    encodeURIComponent(info.selectionText) +
    "&url=" +
    encodeURIComponent(info.pageUrl);

  chrome.tabs.create({
    url:
      "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
      encodeURIComponent(composeRoute),
  });
}

/**
 * Opens Buffer's post composer with image
 */
async function openPublishWithImage(info, tab) {
  const title = await getTabContent(tab.id);

  chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
    var tab = tabs[0];
    let url = tab.url;

    const data = {
      text: title,
      url: url,
      picture: info.srcUrl,
      placement: "hover_button_image",
    };

    var publishURL = new URL("https://publish.buffer.com/compose");
    publishURL.search = new URLSearchParams(data);
    chrome.tabs.create({
      url:
        "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
        encodeURIComponent(publishURL.toString()),
    });
  });
}

/**
 * Opens Buffer's idea composer with current tab content
 */
async function openIdeasWithTab(tabId) {
  const title = await getTabContent(tabId);

  chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
    var tab = tabs[0];
    let text = title + " " + tab.url;

    const data = {
      text: text,
    };

    var publishURL = new URL("https://publish.buffer.com/content/new");
    publishURL.search = new URLSearchParams({ ...data, source: "extension" });
    chrome.tabs.create({
      url:
        "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
        encodeURIComponent(publishURL.toString()),
    });
  });
}

/**
 * Opens Buffer's idea composer with selected text
 */
function openIdeasWithSelection(info) {
  let text = info.selectionText + " " + info.pageUrl;
  let composeRoute =
    "https://publish.buffer.com/content/new?source=extension&text=" +
    encodeURIComponent(text);

  chrome.tabs.create({
    url:
      "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
      encodeURIComponent(composeRoute),
  });
}

/**
 * Opens Buffer's idea composer with image
 */
async function openIdeasWithImage(info, tab) {
  const title = await getTabContent(tab.id);

  chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
    var tab = tabs[0];
    let text = title + " " + tab.url;

    const data = {
      text: text,
      'media[]': info.srcUrl,
    };

    var publishURL = new URL("https://publish.buffer.com/content/new");
    publishURL.search = new URLSearchParams(data);
    chrome.tabs.create({
      url:
        "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
        encodeURIComponent(publishURL.toString()),
    });
  });
}

chrome.contextMenus.onClicked.addListener(function (info, tab) {
  const { menuItemId } = info;

  // Create Post actions
  if (menuItemId === "pageContextPublish") return openPublishWithTab(tab.id);
  if (menuItemId === "selectionContextPublish") return openPublishWithSelection(info);
  if (menuItemId === "imageContextPublish") return openPublishWithImage(info, tab);

  // Save Idea actions
  if (menuItemId === "pageContextIdeas") return openIdeasWithTab(tab.id);
  if (menuItemId === "selectionContextIdeas") return openIdeasWithSelection(info);
  if (menuItemId === "imageContextIdeas") return openIdeasWithImage(info, tab);

  // AI Brainstorm actions - opens sidepanel
  if (menuItemId === "pageContextAI" || menuItemId === "selectionContextAI" || menuItemId === "imageContextAI") {
    chrome.sidePanel.open({ windowId: tab.windowId });
    return;
  }
});

/**
 * Inject content scripts based on site type
 */
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  var manifestData = chrome.runtime.getManifest();
  if (manifestData.manifest_version === 3) {
    if (changeInfo.status === "complete") {
      // All Sites
      chrome.scripting.executeScript({
        target: { tabId: tabId, allFrames: true },
        files: [
          "jquery-3.6.0.min.js",
          "buffer-hover-button.js",
          "keymaster.js",
          "buffer-hotkey.js",
        ],
      });

      // Site-specific integrations
      if (/twitter\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-twitter.js"],
        });
      }
      
      if (/x\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-twitter.js"],
        });
      }

      if (/tweetdeck\.twitter\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-tweetdeck.js"],
        });

        chrome.scripting.insertCSS({
          target: { tabId: tabId },
          files: ["buffer-tweetdeck.css"],
        });
      }

      if (/pinterest\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-pinterest.js"],
        });

        chrome.scripting.insertCSS({
          target: { tabId: tabId },
          files: ["buffer-pinterest.css"],
        });
      }

      if (/reddit\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-reddit.js"],
        });
      }

      if (/news\.ycombinator\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-hn.js"],
        });
      }
    }
  }
});

/**
 * Handle messages from content scripts and sidepanel
 */
chrome.runtime.onMessage.addListener(function (message, sender) {
  const { action, data } = message;

  if (action === "PUBLISH") {
    // For idea_node placement, use the provided text content instead of extracting from page
    if (data["url"] && data.placement !== 'hover_button_image' && data.placement !== 'idea_node') {
      chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
        let tab = tabs[0];
        openPublishWithTab(tab.id);
      });
    } else {
      var url = new URL("https://publish.buffer.com/compose");
      url.search = new URLSearchParams(data);
      chrome.tabs.create({
        url:
          "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
          encodeURIComponent(url.toString()),
      });
    }
  } else if (action === "PUBLISHTAB") {
    var tabId = data["tabId"];
    openPublishWithTab(tabId);
  } else if (action === "IDEA") {
    // For idea_node placement, use the provided text content instead of extracting from page
    if (data["url"] && data.placement !== 'hover_button_image' && data.placement !== 'idea_node') {
      chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
        let tab = tabs[0];
        openIdeasWithTab(tab.id);
      });
    } else {
      var url = new URL("https://publish.buffer.com/content/new");
      url.search = new URLSearchParams({ ...data, source: "extension" });
      chrome.tabs.create({
        url:
          "https://login.buffer.com/login?cta=browserExtension-button-1&redirect=" +
          encodeURIComponent(url.toString()),
      });
    }
  } else if (action === "IDEASTAB") {
    var tabId = data["tabId"];
    openIdeasWithTab(tabId);
  } else if (action === "GET_PAGE_CONTENT") {
    var tabId = data["tabId"];
    
    chrome.tabs.get(tabId, (tab) => {
      if (chrome.runtime.lastError) {
        return;
      }

      const cachedEntry = contentCache.getFullEntry(tabId, tab.url);
      if (cachedEntry && cachedEntry.apiResponse) {
        chrome.storage.local.set({
          [`pageContent_${data.requestId}`]: cachedEntry.content,
          [`apiResponse_${data.requestId}`]: cachedEntry.apiResponse
        }, () => {
          chrome.runtime.sendMessage({
            action: "PAGE_CONTENT_RESULT",
            requestId: data.requestId,
            hasCachedApiResponse: true
          });
        });
        return;
      }

      getPageContentForAI(tabId).then(content => {
        chrome.storage.local.set({
          [`pageContent_${data.requestId}`]: content
        }, () => {
          chrome.runtime.sendMessage({
            action: "PAGE_CONTENT_RESULT",
            requestId: data.requestId,
            hasCachedApiResponse: false
          });
        });
      }).catch(error => {
        chrome.storage.local.set({
          [`pageContent_${data.requestId}`]: {
            error: 'Failed to extract content: ' + error.message,
            fullText: '',
            title: '',
            url: '',
            description: '',
            headings: [],
            wordCount: 0
          }
        });
      });
    });
  } else if (action === "CACHE_API_RESPONSE") {
    var tabId = data["tabId"];
    var url = data["url"];
    var apiResponse = data["apiResponse"];
    
    contentCache.setApiResponse(tabId, url, apiResponse);
  } else if (action === "CLEAR_CACHE_FOR_TAB") {
    var tabId = data["tabId"];
    var url = data["url"];
    
    contentCache.clearForTab(tabId, url);
  }
});

/**
 * Set appropriate icon based on system theme
 */
function setIcon() {
  chrome.windows.getCurrent({}, function (currentWindow) {
    var isSystemDark =
      currentWindow.matchMedia &&
      currentWindow.matchMedia("(prefers-color-scheme: dark)").matches;

    var action = chrome.action != null ? chrome.action : chrome.browserAction;

    if (isSystemDark) {
      action.setIcon({
        path: {
          16: "icon16-dark.png",
          32: "icon32-dark.png",
          48: "icon48-dark.png",
          128: "icon128-dark.png",
        },
      });
    }
  });
}

/**
 * Handle theme changes
 */
chrome.runtime.onMessage.addListener((request) => {
  var action = chrome.action != null ? chrome.action : chrome.browserAction;

  action.setIcon({
    path:
      request.scheme === "dark"
        ? {
            16: "icon16-dark.png",
            32: "icon32-dark.png",
            48: "icon48-dark.png",
            128: "icon128-dark.png",
          }
        : {
            16: "icon16.png",
            32: "icon32.png",
            48: "icon48.png",
            128: "icon128.png",
          },
  });
});

/**
 * Initialize default extension settings
 */
function setDefaultSettings() {
  chrome.storage.local.get("migrated", function (result) {
    if (JSON.stringify(result) === "{}") {
      var store = {};
      store["buffer.op.hacker"] = "hacker";
      store["buffer.op.image-overlays"] = "image-overlays";
      store["buffer.op.key-combo"] = "alt+b";
      store["buffer.op.key-enable"] = "key-enable";
      store["buffer.op.pinterest"] = "pinterest";
      store["buffer.op.reddit"] = "reddit";
      store["buffer.op.tweetdeck"] = "tweetdeck";
      store["buffer.op.twitter"] = "twitter";
      store["migrated"] = true;

      chrome.storage.local.set(store, function () {});
    }
  });
}

/**
 * Get tab content (selection or title)
 */
async function getTabContent(tabId) {
  let selection = await getTabSelection(tabId);

  if (selection != null && selection != "") {
    return selection;
  } else {
    let title = await getTabTitle(tabId);
    return title;
  }
}

/**
 * Extract comprehensive page content for AI processing with caching
 */
async function getPageContentForAI(tabId) {
  return new Promise((resolve) => {
    chrome.tabs.get(tabId, (tab) => {
      if (chrome.runtime.lastError) {
        resolve({
          fullText: '',
          title: '',
          url: '',
          description: '',
          headings: [],
          wordCount: 0,
          error: 'Failed to access tab'
        });
        return;
      }

      const tabUrl = tab.url;
      const cachedContent = contentCache.get(tabId, tabUrl);
      if (cachedContent) {
        resolve(cachedContent);
        return;
      }

      chrome.scripting.executeScript(
        {
          target: { tabId: tabId },
          files: ["get-page-content.js"],
        },
        (response) => {
          if (response && response[0] && response[0].result) {
            const extractedContent = response[0].result;
            contentCache.set(tabId, tabUrl, extractedContent);
            resolve(extractedContent);
          } else {
            const fallbackContent = {
              fullText: '',
              title: tab.title || '',
              url: tabUrl,
              description: '',
              headings: [],
              wordCount: 0,
              error: 'Failed to extract content'
            };
            
            contentCache.set(tabId, tabUrl, fallbackContent);
            resolve(fallbackContent);
          }
        }
      );
    });
  });
}

/**
 * Get selected text from tab
 */
function getTabSelection(tabId) {
  return new Promise((resolve) => {
    var manifestData = chrome.runtime.getManifest();

    if (manifestData.manifest_version === 3) {
      chrome.scripting.executeScript(
        {
          target: { tabId: tabId },
          files: ["get-selection.js"],
        },
        (response) => {
          if (response[0] != null) {
            if ("result" in response[0]) {
              if (response[0].result != null) {
                resolve(response[0].result);
              } else {
                resolve(null);
              }
            } else {
              resolve(response[0]);
            }
          } else {
            resolve(null);
          }
        }
      );
    } else {
      chrome.tabs.executeScript(
        tabId,
        {
          code: `
                var selection = document.getSelection().toString();
                [selection]
            `,
        },
        function (response) {
          resolve(response[0]);
        }
      );
    }
  });
}

/**
 * Get page title (preferring og:title)
 */
function getTabTitle(tabId) {
  return new Promise((resolve) => {
    var manifestData = chrome.runtime.getManifest();

    if (manifestData.manifest_version === 3) {
      chrome.scripting.executeScript(
        {
          target: { tabId: tabId },
          files: ["get-title.js"],
        },
        (response) => {
          if (response[0].result != null) {
            resolve(response[0].result);
          } else {
            resolve(response[0]);
          }
        }
      );
    } else {
      chrome.tabs.executeScript(
        tabId,
        {
          code: `
                var title = document.title;
                var ogTitle = document.head && document.head.querySelector('meta[property="og:title"]');
                if (ogTitle && ogTitle.content && ogTitle.content.length) {
                  title = ogTitle.content;
                }
                [title]
            `,
        },
        function (response) {
          resolve(response[0]);
        }
      );
    }
  });
}
