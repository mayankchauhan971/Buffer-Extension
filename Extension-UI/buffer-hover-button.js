/**
 * Buffer share buttons visible on hover
 */
(function () {
  /**
   * Prevent from being inserted in certain situations
   */
  // Do not insert for iframes
  if (window !== window.parent) return;
  // Do no insert for content editing windows
  if (!document.body || document.body.hasAttribute("contenteditable")) return;

  /**
   * Site detection
   */
  var domain = window.location.hostname.replace("www.", "");
  var site = {
    isGmail: /mail.google.com/.test(domain),
    isInstagram: /instagram.com/.test(domain),
    isBluesky: /bsky.app/.test(domain),
  };

  // List of sites to disable this on:
  var disabledDomains = [
    "buffer.com",
    "bufferapp.com",
    "twitter.com",
    "x.com",
    "facebook.com",
  ];
  if (disabledDomains.indexOf(domain) > -1) {
    return;
  }

  /**
   * Create a single global button container
   */
  var currentImageUrl = null;
  var buttonWidth = 67;
  var buttonHeight = 24;
  var backgroundImage = chrome.runtime.getURL("buffer-hover-button.svg");
  var isButtonVisible = false;

  // Wrapper around all of the buttons... Gets shown and hidden based on whether you're hovering over an image.
  var buttonContainer = document.createElement("div");
  buttonContainer.id = "buffer-extension-hover-container";

  buttonContainer.style.cssText = [
    "font-family: 'Roboto', sans-serif;",
    "display: none;",
    "position: absolute;",
    "z-index: 8675309;",
    "cursor:pointer;",
    "width: " + buttonWidth + "px;",
  ].join("");

  // If user is within this container, show the additional options...
  var buttonHoverContainer = document.createElement("div");
  buttonHoverContainer.id = "buffer-extension-button-hover-container";
  buttonHoverContainer.style.cssText = [
    "position: relative;",
    "z-index: 8675309;",
    "width: " + buttonWidth + "px;",
    "height: " + buttonHeight + "px;",
  ].join("");
  buttonContainer.appendChild(buttonHoverContainer);

  // Buffer "Button"
  var button = document.createElement("span");
  button.id = "buffer-extension-hover-button";

  button.style.cssText = [
    "width: " + buttonWidth + "px;",
    "height: " + buttonHeight + "px;",
    "background-image: url(" + backgroundImage + ");",
    "background-size: " + buttonWidth + "px " + buttonHeight + "px;",
    "cursor: pointer;",
    "opacity: 0.9;",
    "display: block;",
    "position: relative;",
    "z-index: 8675309;",
  ].join("");
  buttonHoverContainer.appendChild(button);

  var menuWidth = 140;
  var menuButtonHeight = 28;
  var menuBottom = menuButtonHeight * 2 + 38;

  var buttonMenu = document.createElement("div");
  buttonMenu.id = "buffer-extension-button-menu";
  buttonMenu.style.cssText = [
    "position: relative;",
    "z-index: 8675308;",
    "width: " + menuWidth + "px;",
    "height: " + menuBottom + "px;",
    "display: none;",
    "bottom: " + menuBottom + "px;",
    "right: 75px;",
  ].join("");
  buttonHoverContainer.appendChild(buttonMenu);

  let buttonCSS = [
    "font-size: 14px;",
    "width: " + menuWidth + "px;",
    "height: " + menuButtonHeight + "px;",
    "display: block;",
    "cursor: pointer;",
    "padding: 0 4px;",
    "color:#3D3D3D;",
    "line-height: " + menuButtonHeight + "px;",
  ];

  var spacerTop = document.createElement("div");
  spacerTop.style.cssText = buttonCSS
    .concat([
      "background-color:rgba(255, 255, 255, 0.8);",
      "-webkit-border-top-left-radius: 4px;",
      "-webkit-border-top-right-radius: 4px;",
      "-moz-border-radius-topleft: 4px;",
      "-moz-border-radius-topright: 4px;",
      "border-top-left-radius: 4px;",
      "border-top-right-radius: 4px;",
      "height:4px;",
    ])
    .join("");
  buttonMenu.appendChild(spacerTop);

  var createPostButton = document.createElement("div");
  createPostButton.id = "buffer-extension-create";
  createPostButton.className = "buffer-extension-hover-button";
  createPostButton.style.cssText = buttonCSS.join("");
  createPostButton.innerHTML = "<span>Create Post</span>";
  buttonMenu.appendChild(createPostButton);

  var saveForLaterButton = document.createElement("div");
  saveForLaterButton.id = "buffer-extension-save";
  saveForLaterButton.className = "buffer-extension-hover-button";
  saveForLaterButton.style.cssText = buttonCSS.join("");
  saveForLaterButton.innerHTML = "<span>Save Idea</span>";
  buttonMenu.appendChild(saveForLaterButton);

  var spacer = document.createElement("div");
  spacer.style.cssText = buttonCSS
    .concat([
      "background-color:rgba(255, 255, 255, 0.8);",
      "-webkit-border-bottom-right-radius: 4px;",
      "-webkit-border-bottom-left-radius: 4px;",
      "-moz-border-radius-bottomright: 4px;",
      "-moz-border-radius-bottomleft: 4px;",
      "border-bottom-right-radius: 4px;",
      "border-bottom-left-radius: 4px;",
      "height:4px;",
    ])
    .join("");
  buttonMenu.appendChild(spacer);

  var styleSheet = document.createElement("style");
  styleSheet.innerHTML =
    ".buffer-extension-hover-button { background-color:rgba(255, 255, 255, 0.8); } .buffer-extension-hover-button span { display:block; padding: 0 4px; border-radius: 4px 4px 4px 4px; -webkit-border-radius: 4px 4px 4px 4px; -moz-border-radius: 4px 4px 4px 4px; } .buffer-extension-hover-button:hover span { background-color:#F5F5F5; }";
  buttonMenu.appendChild(styleSheet);

  var offset = 5;
  var image;
  var box;

  var showButton = function (e) {
    image = e.target;
    var imageUrl = getImageUrl(image);

    if (!isValidImageUrl(imageUrl)) return;

    box = image.getBoundingClientRect();
    if (box.height < 250 || box.width < 350) return;

    buttonContainer.style.display = "block";
    currentImageUrl = imageUrl;
    isButtonVisible = true;
  };

  var locateButton = function () {
    box = image.getBoundingClientRect();

    // Use image.width and height if available
    var width = image.width || box.width,
      height = image.height || box.height,
      extraXOffset = 0,
      extraYOffset = 0;

    // In Gmail, we slide over the button for inline images to not block g+ sharing
    if (
      site.isGmail &&
      window.getComputedStyle(image).getPropertyValue("position") !== "absolute"
    ) {
      extraXOffset = 83;
      extraYOffset = 4;
    }

    // Bluesky iamges with an alt tag render a small [alt]
    // badge which is around 14px wide, so we set to 24 to
    // account for it + a bit of padding
    if (site.isBluesky && image.getAttribute('alt').length > 0) {
      extraXOffset = 22;
      extraYOffset = -2;
    }

    var x =
      window.pageXOffset +
      box.left +
      width -
      buttonWidth -
      offset -
      extraXOffset;
    var y =
      window.pageYOffset +
      box.top +
      height -
      buttonHeight -
      offset -
      extraYOffset;

    // If body is positioned, the button will be positioned against it. So, if body is positioned and shifted
    // up or down, or is being shifted up or down by a children having a top margin other than 0, account for
    // that additional vertical offset.
    var isBodyPositioned =
      window.getComputedStyle(document.body).getPropertyValue("position") !=
      "static";
    if (isBodyPositioned) {
      var bodyTopOffset =
        document.body.getBoundingClientRect().top + window.pageYOffset;
      y -= bodyTopOffset;
    }

    buttonContainer.style.top = y + "px";
    buttonContainer.style.left = x + "px";
  };

  // When hovering the button Container...
  var hoverButtonContainer = function () {
    buttonContainer.style.opacity = "1.0";
    buttonContainer.style.display = "block";

    buttonMenu.style.display = "block";
  };

  var onMouseLeaveButtonContainer = function () {
    buttonContainer.style.display = "none";
    buttonContainer.style.opacity = "0.9";

    buttonMenu.style.display = "none";

    isButtonVisible = false;
  };

  var hideButton = function (e) {
    buttonContainer.style.display = "none";
    buttonContainer.style.opacity = "0.6";
    isButtonVisible = false;
  };

  var onImageMouseEnter = function (e) {
    showButton(e);
    locateButton();
  };

  var onScroll = function () {
    if (isButtonVisible) locateButton();
  };

  var bufferImage = function (e) {
    if (!currentImageUrl) return;

    let title = getImageAltTextAndPageTitle() || getPageTitle();

    e.preventDefault();

    const message = {
      data: {
        picture: currentImageUrl,
        text: title,
        url: window.location.href,
        placement: "hover_button_image",
      },
      action: "PUBLISH",
    };
    chrome.runtime.sendMessage(message);
  };

  var saveImage = function (e) {
    if (!currentImageUrl) return;

    e.preventDefault();

    let title = getImageAltTextAndPageTitle() || getPageTitle();

    const message = {
      data: {
        'media[]': currentImageUrl,
        text: title + " " + window.location.href,
        placement: "hover_button_image",
      },
      action: "IDEA",
    };
    chrome.runtime.sendMessage(message);
  };

  var count = 0;
  $(buttonHoverContainer, buttonMenu)
    .mouseenter(function () {
      count++;
      hoverButtonContainer();
    })
    .mouseleave(function () {
      count--;
      if (!count) {
        onMouseLeaveButtonContainer();
      }
    });

  $(button).on("click", bufferImage);

  $(createPostButton).on("click", bufferImage);

  $(saveForLaterButton).on("click", saveImage);

  var getImageUrl = (function (domain) {
    if (site.isInstagram) {
      return function (el) {
        return el.style.backgroundImage.replace("url(", "").replace(")", "");
      };
    }

    return function (el) {
      return el.src;
    };
  })(domain);

  function getPageTitle() {
    var title = document.title;
    var ogTitle =
      document.head && document.head.querySelector('meta[property="og:title"]');
    if (ogTitle && ogTitle.content && ogTitle.content.length) {
      title = ogTitle.content;
    }
    return title;
  }

  function getImageAltTextAndPageTitle() {
    if (image && image.alt && image.alt.length > 10) {
      if (site.isBluesky) {
        return `${image.alt} / ${getPageTitle()}`
      }
      return `${image.alt} (${getPageTitle()})`
    }
  }

  var addBufferImageOverlays = function () {
    var selector = "img";

    if (site.isInstagram) {
      selector = ".Image.timelinePhoto, .Image.Frame";
    }

    document.body.appendChild(buttonContainer);

    $(document)
      .on("mouseenter", selector, onImageMouseEnter)
      .on("mouseleave", selector, hideButton);

    // scroll events don't bubble, so we listen to them during their capturing phase
    window.addEventListener("scroll", onScroll, true);
  };

  (function check() {
    chrome.storage.local.get("buffer.op.image-overlays", function (result) {
      if (result["buffer.op.image-overlays"] === "image-overlays") {
        addBufferImageOverlays();
      } else {
        setTimeout(check, 2000);
      }
    });
  })();
})();

// Returns true if the extension matches /(jpg|jpeg|gif|png)/, or if url has no extension
var isValidImageUrl = function (url) {
  var imageExtensionMatch = url.match(/\.([a-z]{3,4})(?:[?#:][^/]*)?$/i);
  return (
    imageExtensionMatch === null ||
    /(jpg|jpeg|gif|png)/i.test(imageExtensionMatch[1])
  );
};
