/**
 * The options page is displayed within the browser's extensions page on Chrome
 * and Firefox, and needs less prominent styling then. The options page can
 * also be open as a separate tab though, and full styling is needed then. To enable
 * this workflow, we load a base stylesheet in all cases, that contains the complete
 * collection of styles, and we load a smaller stylesheet that overrides some of
 * those styles when the options page is embedded in a frame.
 */
var isFramed = window.location.hash !== '#newtab';
var framedStylesheet = document.querySelector('[data-use-when="framed"]');
if (isFramed) framedStylesheet.setAttribute('href', framedStylesheet.getAttribute('data-href'));

$(document).ready(function() {
  var currentBrowser = (
    location.protocol === 'chrome-extension:' ? 'chrome' :
    location.protocol === 'moz-extension:' ? 'firefox' :
    location.protocol === 'safari-web-extension:' ? 'safari' : ''
  );

  // Dynamically update page title and email subject
  var hardcodedBrowserName = 'Chrome';
  var formattedCurrentBrowser = currentBrowser.substr(0, 1).toUpperCase() + currentBrowser.substr(1);
  document.title = document.title.replace(hardcodedBrowserName, formattedCurrentBrowser);
  var emailAnchor = document.getElementById('email-anchor');
  emailAnchor.href = emailAnchor.href.replace(hardcodedBrowserName, formattedCurrentBrowser);

  /**
   * Remove Firefox-specific option in other browsers
   */
  if (currentBrowser !== 'firefox') {
    $('#firefox-data-collection').remove();
  }

  /**
   * Turn checkboxes on & off from localStorage based on their values
   */
  var checkboxes = $('input[type="checkbox"]').each(function() {
    var $this = $(this);
    var name = $this.attr('name');

    if (name !== 'firefox-disable-data-collection') {
      var val = $this.attr('value'),
        key = 'buffer.op.' + val;

      chrome.storage.local.get(key, function(result) {
        if (result[key] === val || JSON.string(result[key]) === "{}") {
          $this.attr('checked', true);
        } else {
          $this.attr('checked', false);
        }
      });
      // Specific deserialization for this Firefox data collection setting
    } else {
      chrome.storage.local.get(['buffer.op.firefox-disable-data-collection'], function(result) {
        if (result['buffer.op.firefox-disable-data-collection'] === 'yes') $this.attr('checked', true);
        else $this.attr('checked', false);
      });
    }

  });

  /**
   * Turn radios on & off from localStorage based on their values
   */
  var radios = $('input[type="radio"]').each(function() {
    var $this = $(this);
    var key = 'buffer.op.' + $this.attr('name');
    var val = $this.val();

    chrome.storage.local.get(key, function(result) {
      if (result[key] === val) {
        $this.prop('checked', true);
      }
    });
  });

  /**
   * Fill in text inputs from localStorage
   */
  var inputs = $('input[type="text"]').each(function() {

    var val = $(this).attr('placeholder'),
      key = 'buffer.op.' + $(this).attr('name');

    chrome.storage.local.get(key, function(result) {
      if (result[key] !== val) {
        $(this).val(result[key]);
      }
    });
  });

  /**
   * Clean up the key combo
   */
  $('input[name="key-combo"]').change(function() {
    var val = $(this)
      .val()
      .trim()
      .toLowerCase()
      .replace(/[\s\,\&]/gi, '+') // Convert possible spacer characters to pluses
      .replace(/[^a-z0-9\+]/gi, ''); // Strip out almost everything else
    $(this).val(val);
  });

  /**
   * Save it all
   */
  $('.submit').click(function(ev) {

    ev.preventDefault();

    var keycombo = $('input[name="key-combo"]').val();

    // Check the key combo
    // The regex:
    //
    // starts with (one or more) of     alt / shift / ctrl / command and a plus
    // then (zero or more) of           a-z / 0-9 (only one) and a plus
    // then ends with                   a-z / 0-9 (only one)
    if (keycombo.length > 0 &&
      keycombo.match(/^(((alt|shift|ctrl|command)\+)+([a-z0-9]{1}\+)*([a-z0-9]{1}))$/gi) === null) {
      // Indicate there's an error
      $('input[name="key-combo"]').addClass('error').one('change', function() {
        $(this).removeClass("error");
      });
      // Wiggle the box
      var button = $(this).addClass('wiggle');
      setTimeout(function() {
        $(button).removeClass('wiggle');
      }, 1500 * 0.15);
      return;
    }


    var store = {};

    // Save the checkbox values based on their values
    $(checkboxes).each(function() {
      var $this = $(this);
      var name = $this.attr('name');

      if (name !== 'firefox-disable-data-collection') {
        var val = $this.attr('value'),
          key = 'buffer.op.' + val;

        if ($this.prop('checked')) {
          store[key] = val;
        } else {
          store[key] = false;
        }
        // Specific serialization for this Firefox data collection setting
      } else {
        var settingValue = $this.prop('checked') ? 'yes' : 'no';
        store['buffer.op.firefox-disable-data-collection'] = settingValue;
      }

    });

    // Save the radio values based on their values
    $(radios)
      .filter(function() {
        return $(this).is(':checked');
      })
      .each(function() {
        var $this = $(this);
        var key = 'buffer.op.' + $this.attr('name');
        var val = $this.val();

        store[key] = val;
      });

    // Save the checkbox values based on their values
    $(inputs).each(function() {

      var val = $(this).val(),
        key = 'buffer.op.' + $(this).attr('name'),
        placeholder = $(this).attr('placeholder');

      if (val !== placeholder) {
        if (val === '') val = placeholder;
        store[key] = val;
      }

    });

    chrome.storage.local.set(store, function() {
      // Indicate to the user that things went well
      $(this).text('Saved').addClass("saved");

      // Ask for a good review
      if (!localStorage.getItem('buffer.reviewed')) {
        var webstoresCopy = {
          chrome: 'the <a class="fivestars" href="https://chrome.google.com/webstore/detail/buffer/noojglkidnpfjbincgijbaiedldjfbhh" target="_bank">Chrome Web Store</a>',
          firefox: '<a class="fivestars" href="https://addons.mozilla.org/en-US/firefox/addon/buffer-for-firefox/" target="_bank">Mozilla Add-ons</a>',
          safari: '<a class="fivestars" href="https://apps.apple.com/us/app/buffer-social-media-composer/id1474298973?mt=12" target="_bank">Mac App Store</a>',
        };

        $('header h1:not(.highlight)').html('Enjoying <strong>Buffer</strong>? Head over to ' + webstoresCopy[currentBrowser] + ' and give us 5 stars.<br>We will love you <em>forever</em>.').addClass('highlight');
      }
    });
  });

  $('#keyboard-shortcuts').click(function(ev) {
    ev.preventDefault();
    chrome.tabs.create({ url: "chrome://extensions/shortcuts" });
  });

  /**
   * Reset the save button
   */
  $('input').on('keyup click', function() {
    $('.submit').text('Save').removeClass("saved");
  });

  /**
   * Store that the user has already been to the Web Store (:. we <3 them)
   */
  $('body').on('click', '.fivestars', function() {
    localStorage.setItem('buffer.reviewed', 'true');
    $('header h1').html("You're <strong>awesome</strong> &hearts;").addClass('love').removeClass('highlight');
  });
});
