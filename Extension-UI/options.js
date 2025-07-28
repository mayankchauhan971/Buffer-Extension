/**
 * Extension Options Page Controller
 * Handles loading/saving extension settings and browser-specific adaptations
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

  // Update page title and email link for current browser
  var hardcodedBrowserName = 'Chrome';
  var formattedCurrentBrowser = currentBrowser.substr(0, 1).toUpperCase() + currentBrowser.substr(1);
  document.title = document.title.replace(hardcodedBrowserName, formattedCurrentBrowser);
  var emailAnchor = document.getElementById('email-anchor');
  emailAnchor.href = emailAnchor.href.replace(hardcodedBrowserName, formattedCurrentBrowser);

  // Remove Firefox-specific option in other browsers
  if (currentBrowser !== 'firefox') {
    $('#firefox-data-collection').remove();
  }

  /**
   * Load checkbox values from storage
   */
  var checkboxes = $('input[type="checkbox"]').each(function() {
    const checkboxEl = $(this);
    var name = checkboxEl.attr('name');

    if (name !== 'firefox-disable-data-collection') {
      var val = checkboxEl.attr('value'),
        key = 'buffer.op.' + val;

      chrome.storage.local.get(key, function(result) {
        if (result[key] === val || JSON.string(result[key]) === "{}") {
          checkboxEl.attr('checked', true);
        } else {
          checkboxEl.attr('checked', false);
        }
      });
    } else {
      chrome.storage.local.get(['buffer.op.firefox-disable-data-collection'], function(result) {
        if (result['buffer.op.firefox-disable-data-collection'] === 'yes') checkboxEl.attr('checked', true);
        else checkboxEl.attr('checked', false);
      });
    }
  });

  /**
   * Load radio button values from storage
   */
  var radios = $('input[type="radio"]').each(function() {
    const radioEl = $(this);
    var key = 'buffer.op.' + radioEl.attr('name');
    var val = radioEl.val();

    chrome.storage.local.get(key, function(result) {
      if (result[key] === val) {
        radioEl.prop('checked', true);
      }
    });
  });

  /**
   * Load text input values from storage
   */
  var inputs = $('input[type="text"]').each(function() {
    const inputEl = $(this);

    var val = inputEl.attr('placeholder'),
      key = 'buffer.op.' + inputEl.attr('name');

    chrome.storage.local.get(key, (result) => {
      if (result[key] !== val) {
        inputEl.val(result[key]);
      }
    });
  });

  /**
   * Clean up keyboard shortcut format
   */
  $('input[name="key-combo"]').change(function() {
    const keyComboInput = $(this);
    var val = keyComboInput
      .val()
      .trim()
      .toLowerCase()
      .replace(/[\s\,\&]/gi, '+')
      .replace(/[^a-z0-9\+]/gi, '');
    keyComboInput.val(val);
  });

  /**
   * Save all settings
   */
  $('.submit').click(function(ev) {
    const submitButton = $(this);
    ev.preventDefault();

    var keycombo = $('input[name="key-combo"]').val();

    // Validate key combo format
    if (keycombo.length > 0 &&
      keycombo.match(/^(((alt|shift|ctrl|command)\+)+([a-z0-9]{1}\+)*([a-z0-9]{1}))$/gi) === null) {
      
      $('input[name="key-combo"]').addClass('error').one('change', function() {
        const keyComboInput = $(this);
        keyComboInput.removeClass("error");
      });
      
      var button = submitButton.addClass('wiggle');
      setTimeout(function() {
        $(button).removeClass('wiggle');
      }, 1500 * 0.15);
      return;
    }

    var store = {};

    // Save checkbox values
    $(checkboxes).each(function() {
      var checkboxEl = $(this);
      var name = checkboxEl.attr('name');

      if (name !== 'firefox-disable-data-collection') {
        var val = checkboxEl.attr('value'),
          key = 'buffer.op.' + val;

        if (checkboxEl.prop('checked')) {
          store[key] = val;
        } else {
          store[key] = false;
        }
      } else {
        var settingValue = checkboxEl.prop('checked') ? 'yes' : 'no';
        store['buffer.op.firefox-disable-data-collection'] = settingValue;
      }
    });

    // Save radio values
    $(radios)
      .filter(function() {
        const radioEl = $(this);
        return radioEl.is(':checked');
      })
      .each(function() {
        var radioEl = $(this);
        var key = 'buffer.op.' + radioEl.attr('name');
        var val = radioEl.val();

        store[key] = val;
      });

    // Save text input values
    $(inputs).each(function() {
      const inputEl = $(this);
      var val = inputEl.val(),
        key = 'buffer.op.' + inputEl.attr('name'),
        placeholder = inputEl.attr('placeholder');

      if (val !== placeholder) {
        if (val === '') val = placeholder;
        store[key] = val;
      }
    });

    chrome.storage.local.set(store, function() {
      submitButton.text('Saved').addClass("saved");

      // Show review request
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
   * Reset save button state when settings change
   */
  $('input').on('keyup click', function() {
    $('.submit').text('Save').removeClass("saved");
  });

  /**
   * Track review completion
   */
  $('body').on('click', '.fivestars', function() {
    localStorage.setItem('buffer.reviewed', 'true');
    $('header h1').html("You're <strong>awesome</strong> &hearts;").addClass('love').removeClass('highlight');
  });
});
