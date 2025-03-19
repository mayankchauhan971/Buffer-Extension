/* Initial Setup
–––––––––––––––––––––––––––––––––––––––––––––––––– */

init();

function init() {
  // By default set the onClicked action.
  var action = chrome.action != null ? chrome.action : chrome.browserAction;
  action.setPopup({ popup: "popup.html" });

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
    // Check if we've performed the migration from localStorage to chrome.storage.
    chrome.storage.local.get("migrated", function (result) {
      if (JSON.stringify(result) === "{}") {
        chrome.windows.getCurrent({}, function (currentWindow) {
          var popupWidth = Math.min(740, currentWindow.width);
          var popupHeight = Math.min(700, currentWindow.height);
          var popupLeft = Math.round((currentWindow.width - popupWidth) / 2);
          var popupTop = Math.round((currentWindow.height - popupHeight) / 2);

          // width and height set to 1 as we don't really need to show the user anything...
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

/* Key Commands
–––––––––––––––––––––––––––––––––––––––––––––––––– */
if (chrome.commands) {
  chrome.commands.onCommand.addListener((command, tab) => {
    if (command === "share-to-buffer-action") openPublishWithTab(tab.id);
  });
}

/* Context Menus
–––––––––––––––––––––––––––––––––––––––––––––––––– */
function setContextMenus() {
  chrome.contextMenus.removeAll();

  // Page
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

  // Selection
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
    type: "separator",
    id: "selectionDivider",
    contexts: ["selection"],
  });
  
  // Image
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
}

// Publish

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

// Ideas

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

async function openIdeasWithImage(info, tab) {
  const title = await getTabContent(tab.id);

  chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
    var tab = tabs[0];

    let text = title + " " + tab.url;

    const data = {
      text: text,
      media: info.srcUrl,
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
  const { menuItemId, linkUrl, pageUrl } = info;

  // Publish Compose
  if (menuItemId === "pageContextPublish") return openPublishWithTab(tab.id);
  if (menuItemId === "selectionContextPublish")
    return openPublishWithSelection(info);
  if (menuItemId === "imageContextPublish")
    return openPublishWithImage(info, tab);

  // Ideas
  if (menuItemId === "pageContextIdeas") return openIdeasWithTab(tab.id);
  if (menuItemId === "selectionContextIdeas")
    return openIdeasWithSelection(info);
  if (menuItemId === "imageContextIdeas") return openIdeasWithImage(info, tab);
});

/* Inject
–––––––––––––––––––––––––––––––––––––––––––––––––– */

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

      // Specific Sites
      // Twitter
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

      // Tweetdeck
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

      // Pinterest
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

      // Reddit
      if (/reddit\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-reddit.js"],
        });
      }

      // HackerNews
      if (/news\.ycombinator\.com/.test(tab.url)) {
        chrome.scripting.executeScript({
          target: { tabId: tabId },
          files: ["buffer-hn.js"],
        });
      }
    }
  }
});

/* Handle messages from embeds...
–––––––––––––––––––––––––––––––––––––––––––––––––– */

chrome.runtime.onMessage.addListener(function (message, sender) {
  const { action, data } = message;

  if (action === "PUBLISH") {
    if (data["url"]) {
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
    if (data["url"]) {
      chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
        let tab = tabs[0];
        openIdeasWithTab(tabId);
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
  }
});

/* Dark Mode Icon
–––––––––––––––––––––––––––––––––––––––––––––––––– */
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

// Light/Dark Mode
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

/* Default Settings
–––––––––––––––––––––––––––––––––––––––––––––––––– */

function setDefaultSettings() {
  chrome.storage.local.get("migrated", function (result) {
    if (JSON.stringify(result) === "{}") {
      var store = {};
      store["buffer.op.hacker"] = "hacker";
      store["buffer.op.image-overlays"] = "image-overlays";
      store["buffer.op.key-combo"] = "alt+b";
      store["buffer.op.key-enable"] = "key-enable";
      store["buffer.op.pinterest"] = "pinterest";
      store["buffer.op.pinterest"] = "pinterest";
      store["buffer.op.reddit"] = "reddit";
      store["buffer.op.tweetdeck"] = "tweetdeck";
      store["buffer.op.twitter"] = "twitter";
      store["migrated"] = true;

      chrome.storage.local.set(store, function () {});
    }
  });
}

async function getTabContent(tabId) {
  // Based on the manifest version
  // Check if the user has something selected on the specific tab.
  // If nothing is selected then fetch the document title or the og:title.
  // og:title often excludes the site name and is preferred by users.

  let selection = await getTabSelection(tabId);

  if (selection != null && selection != "") {
    return selection;
  } else {
    let title = await getTabTitle(tabId);
    return title;
  }
}

/* Get Tab Selection (if available)
–––––––––––––––––––––––––––––––––––––––––––––––––– */
function getTabSelection(tabId) {
  return new Promise((resolve) => {
    var manifestData = chrome.runtime.getManifest();

    if (manifestData.manifest_version === 3) {
      // func doesn't seem to work in Safari:
      // https://developer.apple.com/forums/thread/714225

      chrome.scripting.executeScript(
        {
          target: { tabId: tabId },
          files: ["get-selection.js"],
        },
        (response) => {
          // Chrome and Safari handle the response differently...
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

/* Get/Adjust Title from Tab
–––––––––––––––––––––––––––––––––––––––––––––––––– */
function getTabTitle(tabId) {
  return new Promise((resolve) => {
    var manifestData = chrome.runtime.getManifest();

    if (manifestData.manifest_version === 3) {
      // func doesn't seem to work in Safari:
      // https://developer.apple.com/forums/thread/714225
      chrome.scripting.executeScript(
        {
          target: { tabId: tabId },
          files: ["get-title.js"],
        },
        (response) => {
          // Chrome and Safari handle the response differently...
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
