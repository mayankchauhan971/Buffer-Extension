;(function() {

  /**
   * On Safari, this gets injected in all pages. Since we've got a somewhat intensive
   * loop in there, only run the script on Pinterest sites:
   *
   * - www.pinterest.com
   * - www.pinterest.pt
   * - www.pinterest.de
   * - ...
   */
  var hostname = document.location.hostname;
  if (!/^([^\.]+\.)?pinterest\.(com|pt|de|com\.mx|ie|co\.uk|fr|es|nl|ca|dk|at|se|ch|jp|nz|com\.au)$/.test(hostname)) return;

  /**
   * Example attribute value (contains both html tags and html entities):
   *
   * "The 10 Buffer Values and How We Act on Them Every Day <a href=\"https://open.buffer.com/buffer-values/?utm_content=buffer3e42a&utm_medium=social&utm_source=pinterest.com&utm_campaign=buffer\" rel=\"nofollow\" target=\"_blank\">open.buffer.com/...</a> / Here&#39;s how to cook..."
   */
  var getDecodedAttribute = function(element, attrName) {
    var attrValue = element.getAttribute(attrName);

    // Strip html tags from attr value (Pinterest stores some html tags in attributes)
    // Using a regex is by no means perfect, but Pinterest only stores simple html tags
    // in there, so it should be solid enough.
    attrValue = attrValue.replace(/<\/?[^>]+(>|$)/g, '');

    // Decode html entities
    attrValue = decodeURIComponent(attrValue);

    return attrValue;
  };

  var config = [{
      placement: 'action-items-menu-feed',
      // Classes get overriden on each render here, so we can't rely on the presence of .buffer-inserted
      selector: 'div:contains("Copy link"):parent:parent:first:not(:has(> .BufferButton))',
      $button: $([
        '<div class="BufferButton Jea MtH gjz jzS zI7 iyn Hsu" style="width: 72px;"><div class="oy8 zI7 iyn Hsu zmN" style="width: auto;"><div class="gjz jzS mQ8 un8 TB_" style="width: 48px;"><button aria-label="Copy link" class="HEm adn yQo lnZ wsz YbY" tabindex="0" type="button"><div class="rYa kVc adn yQo czT qrs BG7"><div class="x8f INd _O1 gjz mQ8 WhU gpV" style="height: 48px; width: 48px;"><svg class="Hn_ gUZ R19 U9O kVc" height="20" width="20" viewBox="0 0 448 512" aria-hidden="true" aria-label="" role="img"><path d="M427.84 380.67l-196.5 97.82a18.6 18.6 0 0 1-14.67 0L20.16 380.67c-4-2-4-5.28 0-7.29L67.22 350a18.65 18.65 0 0 1 14.69 0l134.76 67a18.51 18.51 0 0 0 14.67 0l134.76-67a18.62 18.62 0 0 1 14.68 0l47.06 23.43c4.05 1.96 4.05 5.24 0 7.24zm0-136.53l-47.06-23.43a18.62 18.62 0 0 0-14.68 0l-134.76 67.08a18.68 18.68 0 0 1-14.67 0L81.91 220.71a18.65 18.65 0 0 0-14.69 0l-47.06 23.43c-4 2-4 5.29 0 7.31l196.51 97.8a18.6 18.6 0 0 0 14.67 0l196.5-97.8c4.05-2.02 4.05-5.3 0-7.31zM20.16 130.42l196.5 90.29a20.08 20.08 0 0 0 14.67 0l196.51-90.29c4-1.86 4-4.89 0-6.74L231.33 33.4a19.88 19.88 0 0 0-14.67 0l-196.5 90.28c-4.05 1.85-4.05 4.88 0 6.74z"/></svg></div></div></button></div></div><div class="tBJ dyH iFc dR0 O2T tg7 IZT swG"><div class="zI7 iyn Hsu" style="max-width: 128px; max-height: 50px;">Buffer</div></div></div>'
      ].join('')),
      insert: function(el) {
        var $actions = $(el);
        var $newActionItem = this.$button.clone();

        $actions.append($newActionItem);

        return $newActionItem;
      },
      getData: function(el) {
        var $img = $('div[data-test-id="closeup-body-image-container"] img');

        var image = $img.attr('src');
        var source = $('.linkModuleActionButton').closest('a').attr('href');

        var text = $('.CloseupTitleCard h1').text();
        if (!text) {
          text = $('.CloseupTitleCard h2').text();
        }
        if (!text) {
          text = $('.CloseupTitleCard h3').text();
        }

        // If that didn't work, the Pin may have been open directly (there's no board in the
        // background): there may be a pinterestapp:source meta tag we'll try to get it from
        if (!source) {
          var $meta = $('meta[name="al:ios:url"][content^="pinterest://add_pin/"]');
          if ($meta.length) {
            var metaContent = $meta.attr('content');
            var matches = metaContent.match(/&source_url=([^&]+)/);

            if (matches) source = matches[1];
          }
        }

        if (!source) {
          if (window.location.search) {
            var params = window.location.search.split('&');

            var urlIndex = -1;
            for (var i = 0; i < params.length; i++) {
              if (params[i].indexOf('url=') > -1) {
                urlIndex = i;
                break;
              }
            }

            if (urlIndex > -1) {
              source = decodeURIComponent(params[urlIndex].split("=")[1]);
            }
          }

        }

        return {
          text: text,
          url: source,
          picture: getFullSizeImageUrl(image),
          placement: this.placement
        };
      }
    },
    // 2022 (Opened)
    {
      placement: 'action-items-menu',
      // Classes get overriden on each render here, so we can't rely on the presence of .buffer-inserted
      //selector: 'div[data-test-id="closeup-action-items"] > div:nth-child(2) > div:first > div:first > div:nth-child(2) > div:first > div:first > div:first > div:first > div:first > div:nth-child(2) > div:nth-child(2):not(:has(> .BufferButton))',
      $button: $([
        '<div class="BufferButton Jea MtH gjz jzS zI7 iyn Hsu" style="width: 72px;"><div class="oy8 zI7 iyn Hsu zmN" style="width: auto;"><div class="gjz jzS mQ8 un8 TB_" style="width: 48px;"><button aria-label="Copy link" class="HEm adn yQo lnZ wsz YbY" tabindex="0" type="button"><div class="rYa kVc adn yQo czT qrs BG7"><div class="x8f INd _O1 gjz mQ8 WhU gpV" style="height: 48px; width: 48px;"><svg class="Hn_ gUZ R19 U9O kVc" height="20" width="20" viewBox="0 0 448 512" aria-hidden="true" aria-label="" role="img"><path d="M427.84 380.67l-196.5 97.82a18.6 18.6 0 0 1-14.67 0L20.16 380.67c-4-2-4-5.28 0-7.29L67.22 350a18.65 18.65 0 0 1 14.69 0l134.76 67a18.51 18.51 0 0 0 14.67 0l134.76-67a18.62 18.62 0 0 1 14.68 0l47.06 23.43c4.05 1.96 4.05 5.24 0 7.24zm0-136.53l-47.06-23.43a18.62 18.62 0 0 0-14.68 0l-134.76 67.08a18.68 18.68 0 0 1-14.67 0L81.91 220.71a18.65 18.65 0 0 0-14.69 0l-47.06 23.43c-4 2-4 5.29 0 7.31l196.51 97.8a18.6 18.6 0 0 0 14.67 0l196.5-97.8c4.05-2.02 4.05-5.3 0-7.31zM20.16 130.42l196.5 90.29a20.08 20.08 0 0 0 14.67 0l196.51-90.29c4-1.86 4-4.89 0-6.74L231.33 33.4a19.88 19.88 0 0 0-14.67 0l-196.5 90.28c-4.05 1.85-4.05 4.88 0 6.74z"/></svg></div></div></button></div></div><div class="tBJ dyH iFc dR0 O2T tg7 IZT swG"><div class="zI7 iyn Hsu" style="max-width: 128px; max-height: 50px;">Buffer</div></div></div>'
      ].join('')),
      insert: function(el) {
        var $actions = $(el);
        var $newActionItem = this.$button.clone();

        $actions.append($newActionItem);

        return $newActionItem;
      },
      getData: function(el) {
        var $img = $('div[data-test-id="closeup-body-image-container"] img');

        var image = $img.attr('src');
        var source = $('.linkModuleActionButton').closest('a').attr('href');

        var text = $('.CloseupTitleCard h1').text();
        if (!text) {
          text = $('.CloseupTitleCard h2').text();
        }
        if (!text) {
          text = $('.CloseupTitleCard h3').text();
        }

        // If that didn't work, the Pin may have been open directly (there's no board in the
        // background): there may be a pinterestapp:source meta tag we'll try to get it from
        if (!source) {
          var $meta = $('meta[name="al:ios:url"][content^="pinterest://add_pin/"]');
          if ($meta.length) {
            var metaContent = $meta.attr('content');
            var matches = metaContent.match(/&source_url=([^&]+)/);

            if (matches) source = matches[1];
          }
        }

        if (!source) {
          if (window.location.search) {
            var params = window.location.search.split('&');

            var urlIndex = -1;
            for (var i = 0; i < params.length; i++) {
              if (params[i].indexOf('url=') > -1) {
                urlIndex = i;
                break;
              }
            }

            if (urlIndex > -1) {
              source = decodeURIComponent(params[urlIndex].split("=")[1]);
            }
          }

        }

        return {
          text: text,
          url: source,
          picture: getFullSizeImageUrl(image),
          placement: this.placement
        };
      }
    }
  ];

  var insertButton = function(target) {
    $(target.selector).each(function(i, el) {
      var $button = target.insert(el);
      $button.on('click', function(e) {
        e.preventDefault();
        e.stopImmediatePropagation();

        const message = { data: target.getData(el), action: "PUBLISH" }
        chrome.runtime.sendMessage(message);
      })
    });
  };

  var insertButtons = function() {
    config.forEach(insertButton);
  };

  var pinterestLoop = function() {
    insertButtons();

    /**
     * Pinterest mutates the DOM on hover, and we need to insert the button after
     * it has done so. To minimize the visual impact of the Buffer button appearing
     * after hovering, we need to insert the button as fast as possible. This value
     * has proven to be a good tradeof between perceived speed and minimizing the
     * performance impact on the overall page.
     */
    setTimeout(pinterestLoop, 50);
  };

  var start = function() {

    // Add class for css scoping
    document.body.classList.add('buffer-pinterest');

    // Start the loop that will watch for new DOM elements
    pinterestLoop();
  };

  // Make sure we get the fullsize image
  // Example src: 'https://s-media-cache-ak0.pinimg.com/236x/55/1f/ac/551fac47c0dacff21f04012cb5c020cf.jpg'
  var getFullSizeImageUrl = function(url) {
    if (typeof url === 'undefined') return;

    var urlParts = url.split('/');
    var isPinterestImage = urlParts[2].indexOf('pinimg.com') != -1;

    // Non-Pinterest images should already be full-size
    if (!isPinterestImage) return url;

    urlParts[3] = '736x'; // 736 is the Pinterest standard for fullsize images
    return urlParts.join('/');
  };

  // Wait for xt.options to be set
  ;(function check() {
    chrome.storage.local.get("buffer.op.pinterest", function(result) {
     if(result["buffer.op.pinterest"] === "pinterest"){
      start();
     } else {
         setTimeout(check, 2000);
     }
   });
  }());

}());