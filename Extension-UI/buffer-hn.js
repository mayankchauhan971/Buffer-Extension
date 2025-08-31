;(function() {

  var config = {};
  config.base = "https://news.ycombinator.com/",
    config.buttons = [{
      text: "add to buffer",
      container: 'td.subtext',
      className: 'buffer-hn-button',
      selector: '.buffer-hn-button',
      data: function(elem) {
        var article = $(elem).parents('tr').prev('tr').find('.title').children('a');
        var title = $(article).text().trim();
        var link = $(article).attr('href').trim();

        return {
          text: title,
          url: link,
          placement: 'hn-add'
        };
      }
    }];

  var createButton = function(btnConfig) {

    var a = document.createElement('a');
    a.setAttribute('class', btnConfig.className);
    a.setAttribute('href', '#');
    $(a).text(btnConfig.text);

    return a;

  };

  var insertButtons = function() {

    var i, l = config.buttons.length;
    for (i = 0; i < l; i++) {

      var btnConfig = config.buttons[i];

      $(btnConfig.container).each(function() {

        var container = $(this);

        if ($(container).hasClass('buffer-inserted')) return;

        $(container).addClass('buffer-inserted');

        var btn = createButton(btnConfig);

        $(container).append(btn);

        $(btn).before(' | ');

        var getData = btnConfig.data;

        $(btn).click(function(e) {
          const message = { data: getData(btn), action: "PUBLISH" }
          chrome.runtime.sendMessage(message);
        });

      });

    }

  };

  ;
  (function check() {
    chrome.storage.local.get("buffer.op.hacker", function(result) {
      if (result["buffer.op.hacker"] === "hacker") {
        insertButtons();
      } else {
        setTimeout(check, 2000);
      }
    });
  }());

}());