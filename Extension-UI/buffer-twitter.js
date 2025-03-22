;(function() {

	// Only run this script on twitter:
	if (window.location.host.indexOf('twitter.com') !== 0 && window.location.host.indexOf('x.com') !== 0) return;

	var buildElement = function buildElement(parentConfig) {

		var temp = document.createElement(parentConfig[0]);
		if (parentConfig[1]) temp.setAttribute('class', parentConfig[1]);
		if (parentConfig[2]) temp.setAttribute('style', parentConfig[2]);

		if (parentConfig.length > 3) {
			var i = 3,
				l = parentConfig.length;
			for (; i < l; i++) {
				temp.appendChild(buildElement(parentConfig[i]));
			}
		}

		return temp;

	};

	var config = {};
	config.time = {
		success: {
			delay: 2000
		}
	};

	var getTextFromRichtext = function(html) {
		var text;

		// Browsers have different ways of handling contenteditable divs, and the
		// underlying markup is different as a result. Blink/Webkit use divs, Firefox
		// uses brs. ps are here for legacy reasons and can be removed in a few months.
		html = html
			.replace(/<div><br><\/div>/gi, '\n')
			.replace(/<\/div>(\s?)<div>/gi, '\n$1')
			.replace(/<\/p>/gi, '\n')
			.replace(/<br>(?!$)/gi, '\n')
			.replace(/<br>(?=$)/gi, '');

		text = $('<div>')
			.html(html)
			.find('[data-pictograph-text]')
			.replaceWith(function() {
				return $(this).attr('data-pictograph-text');
			})
			.end()
			.text();

		return text;
	};

	config.buttons = [{
			// "New Twitter" Timeline (Updated September 10th 2021)
			name: "buffer-timeline-sept-2021",
			text: "Add to Buffer",
			container: "article[data-testid='tweet'] div[role='group']",
			after: "div:first",
			default: '',
			selector: '.buffer-action',
			location: 'timeline',
			create: function(btnConfig) {
				var action = document.createElement('div');
				action.className = 'buffer-timeline-sept-2021 ProfileTweet-action--buffer js-toggleState';

				var button = document.createElement('button');
				button.style.backgroundColor = 'none';
				button.style.background = "none";
				button.style.border = "none";
				button.style.marginTop = "5px";
				button.style.marginRight = "47px";
				button.style.cursor = "pointer";
				button.className = 'ProfileTweet-actionButton js-actionButton';
				button.type = 'button';

				var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
				setAttributes(svg, {
					"viewBox": "0 0 22 22",
					"height": "22px",
					"version": "1.1",
					"fill": "none",
				});

				var ellipseAttr = {
					"stroke-miterlimit": "20",
					"stroke-linecap": "round",
					"stroke-linejoin": "round",
					"stroke": "#657786",
				};

				var ellipse = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M1.28205 5.33333L9.84615 9.46154C9.94872 9.51282 10.0513 9.51282 10.1538 9.46154L18.7179 5.33333C19 5.20513 19 4.82051 18.7179 4.69231L10.1538 0.538462C10.0513 0.487179 9.94872 0.487179 9.84615 0.538462L1.28205 4.66667C1 4.79487 1 5.20513 1.28205 5.33333Z";
				setAttributes(ellipse, ellipseAttr);
				svg.appendChild(ellipse);

				var ellipse2 = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M1.28205 10.3333L9.84615 14.4615C9.94872 14.5128 10.0513 14.5128 10.1538 14.4615L18.7179 10.3333C19 10.2051 19 9.82051 18.7179 9.6923L16.9231 8.82051L11.1795 11.5641C10.8205 11.7436 10.4103 11.8205 10 11.8205C9.58974 11.8205 9.17949 11.7179 8.82051 11.5641L3.07692 8.79487L1.28205 9.66666C1 9.79487 1 10.2051 1.28205 10.3333Z";
				setAttributes(ellipse2, ellipseAttr);
				svg.appendChild(ellipse2);

				var ellipse3 = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M18.7179 14.6667L16.9231 13.7949L11.1795 16.5641C10.8205 16.7436 10.4103 16.8205 10 16.8205C9.58974 16.8205 9.17949 16.7179 8.82051 16.5641L3.07692 13.7949L1.28205 14.6667C1 14.7949 1 15.1795 1.28205 15.3077L9.84615 19.4359C9.94872 19.4872 10.0513 19.4872 10.1538 19.4359L18.7179 15.3333C19 15.2051 19 14.7949 18.7179 14.6667Z";
				setAttributes(ellipse3, ellipseAttr)
				svg.appendChild(ellipse3);

				button.appendChild(svg);
				action.appendChild(button);

				return action;
			},
			data: function(elem) {
				// Find the Tweet container
				var $tweet = $(elem).closest('article');
				var $tweetContent = $tweet.find("div[lang]").first();

				var text = '';

				// Now Twitter splits tweets into spans text, emojis and hashtags, so we need
				// to iterate over all of them to grab the full text.
				$($tweetContent).children('span,a,div').each(function(e) {
					if ($(this).is('div')) { // Link, grab the full URL
						text += $(this).find('span a').text();
						return true;
					}

					if ($(this).is('a')) { // Link, grab the full URL
						text += $(this).attr('href');
						return true;
					}

					if (($(this).attr('dir') === 'auto') && $(this).find('> div').first()) { // Emoji span
						text += $(this).find('> div').first().attr('aria-label');
						return true;
					}

					if ($(this).is('span')) { // Regular text
						text += $(this).text();
						return true;
					}
				});

				text = text.trim();

				// Fetch the single time element, from there we can grab the href from the parent to get the screen name and status id.
				var $link = $tweet.find('time').parent();
				var tweetStatusURL = $link.attr('href');
				// result : /mjtsai/status/1131268140887883779

				var statusID = tweetStatusURL.split(/\//)[3];
				var screenname = tweetStatusURL.split(/\//)[1];

				// Fetch the avatar src which gives us the user id...
				var avatarURL = $tweet.find('img').first().attr('src');
				// result: https://pbs.twimg.com/profile_images/1107099224699740160/hvMb9LQF_bigger.jpg
				var userID = avatarURL.split(/\//)[4];

				// Not depending on dynamic classes, but dom structure may change often...
				// Grab the display name
				var displayName = $tweet.find('a[role=link]:first-child span span').first().text();
				var tweetContentLink = $($tweetContent).find('a[role=link]').first();
				var tweetContentURL = tweetContentLink.attr('href');
				//if not undefined, add a space before the url. If undefined, return empty string
				if (tweetContentURL && !tweetContentURL.startsWith('/')) {
					tweetContentURL = ' ' + tweetContentURL;
				} else {
					tweetContentURL = '';
				}
				// Construct the text...
				var formattedText = 'RT @' + screenname + ': ' + text + tweetContentURL;

				// Send back the data
				return {
					text: formattedText,
					placement: 'twitter-feed',
					retweeted_tweet_id: statusID,
					retweeted_user_id: userID,
					retweeted_user_name: screenname,
					retweeted_user_display_name: displayName,
					retweeted_user_avatar: avatarURL
				};
			},
			clear: function(elem) {},
			activator: function(elem, btnConfig) {
				var $btn = $(elem);

				// Remove extra margin on the last item in the list to prevent overflow
				var moreActions = $btn.siblings('.js-more-tweet-actions').get(0);
				if (moreActions) {
					moreActions.style.marginRight = '0px';
				}

				if ($btn.closest('.in-reply-to').length > 0) {
					$btn.find('i').css({ 'background-position-y': '-21px' });
				}
			}
		},
		{
			// "New Twitter" Individual Tweet
			name: "buffer-individual-tweet-sept-2021",
			text: "Add to Buffer",
			container: "article[data-testid='tweet']:first div[role='group']:last",
			after: 'div:nth-child(3):first',
			default: '',
			selector: '.buffer-action',
			location: 'individual',
			create: function(btnConfig) {
				var action = document.createElement('div');
				action.className = 'buffer-individual-tweet-sept-2021 ProfileTweet-action--buffer js-toggleState';

				var button = document.createElement('button');
				button.style.backgroundColor = 'none';
				button.style.background = "none";
				button.style.border = "none";
				button.style.marginTop = "13px";
				button.style.cursor = "pointer";
				button.className = 'ProfileTweet-actionButton js-actionButton';
				button.type = 'button';

				var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
				setAttributes(svg, {
					"viewBox": "0 0 22 22",
					"height": "22px",
					"version": "1.1",
					"fill": "none",
				});

				var ellipseAttr = {
					"stroke-miterlimit": "20",
					"stroke-linecap": "round",
					"stroke-linejoin": "round",
					"stroke": "#657786",
				};

				var ellipse = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M1.28205 5.33333L9.84615 9.46154C9.94872 9.51282 10.0513 9.51282 10.1538 9.46154L18.7179 5.33333C19 5.20513 19 4.82051 18.7179 4.69231L10.1538 0.538462C10.0513 0.487179 9.94872 0.487179 9.84615 0.538462L1.28205 4.66667C1 4.79487 1 5.20513 1.28205 5.33333Z";
				setAttributes(ellipse, ellipseAttr);
				svg.appendChild(ellipse);

				var ellipse2 = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M1.28205 10.3333L9.84615 14.4615C9.94872 14.5128 10.0513 14.5128 10.1538 14.4615L18.7179 10.3333C19 10.2051 19 9.82051 18.7179 9.6923L16.9231 8.82051L11.1795 11.5641C10.8205 11.7436 10.4103 11.8205 10 11.8205C9.58974 11.8205 9.17949 11.7179 8.82051 11.5641L3.07692 8.79487L1.28205 9.66666C1 9.79487 1 10.2051 1.28205 10.3333Z";
				setAttributes(ellipse2, ellipseAttr);
				svg.appendChild(ellipse2);

				var ellipse3 = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M18.7179 14.6667L16.9231 13.7949L11.1795 16.5641C10.8205 16.7436 10.4103 16.8205 10 16.8205C9.58974 16.8205 9.17949 16.7179 8.82051 16.5641L3.07692 13.7949L1.28205 14.6667C1 14.7949 1 15.1795 1.28205 15.3077L9.84615 19.4359C9.94872 19.4872 10.0513 19.4872 10.1538 19.4359L18.7179 15.3333C19 15.2051 19 14.7949 18.7179 14.6667Z";
				setAttributes(ellipse3, ellipseAttr)
				svg.appendChild(ellipse3);

				button.appendChild(svg);
				action.appendChild(button);

				return action;
			},
			data: function(elem) {
				// Find the Tweet container
				var $tweet = $(elem).closest('article');

				var tweetURL = window.location.pathname;

				var statusID = tweetURL.split(/\//)[3];
				var screenname = tweetURL.split(/\//)[1];

				// Fetch the avatar src which gives us the user id...
				var avatarURL = $tweet.find('img').first().attr('src');
				// result: https://pbs.twimg.com/profile_images/1107099224699740160/hvMb9LQF_bigger.jpg
				var userID = avatarURL.split(/\//)[4];

				// Not relying on dynamic classes, only DOM hierarchy. Still may likely change often...
				// Grab the display name
				var display_name = $tweet.find('a[role=link]:first-child span span').first().text();

				// Grab the status text...
				var textElement = $tweet.find("div[lang]");
				var text = textElement.text();
				// don't display content like link('google.com/test/test') google.com
				if (text && text.includes('(link:')) {
					text = text.replace(/ *\(link[^)]*\) */g, " ");
				}
				if (text.includes('Replying to')) {
					text = textElement.next().text();
				}
				var tweetContentLink = $tweet.find('div:nth-child(4) > div > div > a[role=link]');
				var tweetContentURL = tweetContentLink.attr('href');
				if (tweetContentURL && !tweetContentURL.startsWith('/')) {
					tweetContentURL = ' ' + tweetContentURL;
				} else {
					tweetContentURL = '';
				}
				// Construct the text...
				var formattedText = 'RT @' + screenname + ': ' + text.trim() + tweetContentURL;
				// Send back the data
				return {
					text: formattedText,
					placement: 'twitter-feed',
					retweeted_tweet_id: statusID,
					retweeted_user_id: userID,
					retweeted_user_name: screenname,
					retweeted_user_display_name: display_name,
					retweeted_user_avatar: avatarURL
				};
			},
			clear: function(elem) {},
			activator: function(elem, btnConfig) {
				var $btn = $(elem);

				// Remove extra margin on the last item in the list to prevent overflow
				var moreActions = $btn.siblings('.js-more-tweet-actions').get(0);
				if (moreActions) {
					moreActions.style.marginRight = '0px';
				}

				if ($btn.closest('.in-reply-to').length > 0) {
					$btn.find('i').css({ 'background-position-y': '-21px' });
				}
			}
		},
		{
			// "New Twitter" Individual Tweet Replies
			name: "buffer-individual-tweet-replies-sept-2021",
			text: "Add to Buffer",
			container: "article[data-testid='tweet']:not(:first) div[role='group']",
			after: 'div:nth-child(3):first',
			default: '',
			selector: '.buffer-action',
			location: 'individual',
			create: function(btnConfig) {
				var action = document.createElement('div');
				action.className = 'buffer-individual-tweet-replies-sept-2021 ProfileTweet-action--buffer js-toggleState';

				var button = document.createElement('button');
				button.style.backgroundColor = 'none';
				button.style.background = "none";
				button.style.border = "none";
				button.style.marginTop = "6px";
				button.style.cursor = "pointer";
				button.className = 'ProfileTweet-actionButton js-actionButton';
				button.type = 'button';

				var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
				setAttributes(svg, {
					"viewBox": "0 0 22 22",
					"height": "22px",
					"version": "1.1",
					"fill": "none",
				});

				var ellipseAttr = {
					"stroke-miterlimit": "20",
					"stroke-linecap": "round",
					"stroke-linejoin": "round",
					"stroke": "#657786",
				};

				var ellipse = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M1.28205 5.33333L9.84615 9.46154C9.94872 9.51282 10.0513 9.51282 10.1538 9.46154L18.7179 5.33333C19 5.20513 19 4.82051 18.7179 4.69231L10.1538 0.538462C10.0513 0.487179 9.94872 0.487179 9.84615 0.538462L1.28205 4.66667C1 4.79487 1 5.20513 1.28205 5.33333Z";
				setAttributes(ellipse, ellipseAttr);
				svg.appendChild(ellipse);

				var ellipse2 = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M1.28205 10.3333L9.84615 14.4615C9.94872 14.5128 10.0513 14.5128 10.1538 14.4615L18.7179 10.3333C19 10.2051 19 9.82051 18.7179 9.6923L16.9231 8.82051L11.1795 11.5641C10.8205 11.7436 10.4103 11.8205 10 11.8205C9.58974 11.8205 9.17949 11.7179 8.82051 11.5641L3.07692 8.79487L1.28205 9.66666C1 9.79487 1 10.2051 1.28205 10.3333Z";
				setAttributes(ellipse2, ellipseAttr);
				svg.appendChild(ellipse2);

				var ellipse3 = document.createElementNS("http://www.w3.org/2000/svg", "path");
				ellipseAttr.d = "M18.7179 14.6667L16.9231 13.7949L11.1795 16.5641C10.8205 16.7436 10.4103 16.8205 10 16.8205C9.58974 16.8205 9.17949 16.7179 8.82051 16.5641L3.07692 13.7949L1.28205 14.6667C1 14.7949 1 15.1795 1.28205 15.3077L9.84615 19.4359C9.94872 19.4872 10.0513 19.4872 10.1538 19.4359L18.7179 15.3333C19 15.2051 19 14.7949 18.7179 14.6667Z";
				setAttributes(ellipse3, ellipseAttr)
				svg.appendChild(ellipse3);

				button.appendChild(svg);
				action.appendChild(button);

				return action;
			},
			data: function(elem) {

				// Find the Tweet container
				var $tweet = $(elem).closest('article');

				var $link = $tweet.find('time').parent();
				var tweetURL = $link.attr('href');

				// Handle highlighted reply...
				if (tweetURL == null) {
					var $link = $tweet.find('a[rel="noopener noreferrer"]:last').parent().find('a[role=link]:first');
					tweetURL = $link.attr('href');
				}

				// result : /mjtsai/status/1131268140887883779

				var statusID = tweetURL.split(/\//)[3];
				var screenname = tweetURL.split(/\//)[1];

				// Fetch the avatar src which gives us the user id...
				var avatarURL = $tweet.find('img').first().attr('src');

				// result: https://pbs.twimg.com/profile_images/1107099224699740160/hvMb9LQF_bigger.jpg
				var userID = $tweet.find('a').first().attr('href');

				// Not relying on dynamic classes, only DOM hierarchy. Still may likely change often...
				// Grab the display name
				var display_name = $tweet.find('a[role=link]:first-child span span').first().text();
				// Grab the status text...
				var textElement = $tweet.find("div[lang]");
				var text = textElement.text();
				// don't display content like link('google.com/test/test') google.com
				if (text && text.includes('(link:')) {
					text = text.replace(/ *\(link[^)]*\) */g, " ");
				}
				if (text.includes('Replying to')) {
					text = textElement.next().text();
				}
				var tweetContentLink = $tweet.find('div:nth-child(4) > div > div > a[role=link]');
				var tweetContentURL = tweetContentLink.attr('href');
				if (tweetContentURL && !tweetContentURL.startsWith('/')) {
					tweetContentURL = ' ' + tweetContentURL;
				} else {
					tweetContentURL = '';
				}
				// Construct the text...
				var formattedText = 'RT @' + screenname + ': ' + text.trim() + tweetContentURL;

				// Send back the data
				return {
					text: formattedText,
					placement: 'twitter-feed',
					retweeted_tweet_id: statusID,
					retweeted_user_id: userID,
					retweeted_user_name: screenname,
					retweeted_user_display_name: display_name,
					retweeted_user_avatar: avatarURL
				};
			},
			clear: function(elem) {},
			activator: function(elem, btnConfig) {
				var $btn = $(elem);

				// Remove extra margin on the last item in the list to prevent overflow
				var moreActions = $btn.siblings('.js-more-tweet-actions').get(0);
				if (moreActions) {
					moreActions.style.marginRight = '0px';
				}

				if ($btn.closest('.in-reply-to').length > 0) {
					$btn.find('i').css({ 'background-position-y': '-21px' });
				}
			}
		},
		{
			// Retweet modal window
			name: "retweet",
			text: "Buffer Retweet",
			container: '.tweet-form.RetweetDialog-tweetForm .tweet-button',
			before: '.retweet-action',
			className: 'buffer-tweet-button EdgeButton EdgeButton--primary',
			selector: '.buffer-tweet-button',
			create: function(btnConfig) {
				var button = document.createElement('button');
				button.setAttribute('class', btnConfig.className);

				var spanTweet = document.createElement('span');
				spanTweet.setAttribute('class', 'button-text tweeting-text');
				spanTweet.innerText = btnConfig.text;

				var spanReply = document.createElement('span');
				spanReply.setAttribute('class', 'button-text replying-text');
				spanReply.innerText = btnConfig.text;

				button.appendChild(spanTweet);
				button.appendChild(spanReply);

				return button;
			},
			data: function(elem) {

				var $elem = $(elem);
				var $dialog = $elem.closest('.retweet-tweet-dialog, #retweet-dialog, #retweet-tweet-dialog');
				var $tweet = $dialog.find('.js-actionable-tweet').first();

				var screenname = $tweet.attr('data-screen-name');
				if (!screenname) {
					screenname = $tweet.find('.js-action-profile-name')
						.filter(function(i) { return $(this).text()[0] === '@' })
						.first()
						.text()
						.trim()
						.replace(/^@/, '');
				}
				var $text = $tweet.find('.js-tweet-text').first();
				var text = getFullTweetText($text, screenname);

				var commentHtml = $elem.closest('form.is-withComment').find('.tweet-content .tweet-box').html();
				var comment = commentHtml ? getTextFromRichtext(commentHtml) : '';

				return {
					text: text,
					placement: 'twitter-retweet',
					retweeted_tweet_id: $tweet.attr('data-item-id'),
					retweeted_user_id: $tweet.attr('data-user-id'),
					retweeted_user_name: $tweet.attr('data-screen-name'),
					retweeted_user_display_name: $tweet.attr('data-name'),
					retweet_comment: comment
				};
			},
			activator: function(elem, btnConfig) {
				var $elem = $(elem);
				var $target = $elem.closest('form').find('.tweet-content .tweet-box');

				$target.on('keyup focus blur change paste cut', function(e) {
					setTimeout(function() {
						var isTweetButtonDisabled = $elem.siblings('.retweet-action').is(':disabled');
						$elem.toggleClass('disabled', isTweetButtonDisabled);
					}, 0);
				});
			}
		}

	];
	// Parse a tweet a return text representing it
	// NOTE: some more refactoring can be done here, e.g. taking care of
	// expanding short links in a single place
	var getFullTweetText = function($text, screenName) {
		var $clone = $text.clone();

		// Expand URLs
		$clone.find('a[data-expanded-url]').each(function() {
			this.textContent = this.getAttribute('data-expanded-url');
		});

		// Replace emotes with their unicode representation
		$clone.find('img.twitter-emoji, img.Emoji').each(function(i, el) {
			$(el).replaceWith(el.getAttribute('alt'));
		});

		// Prepend space separator to hidden links
		$clone.find('.twitter-timeline-link.u-hidden').each(function() {
			this.textContent = ' ' + this.textContent;
		});
		return 'RT @' + screenName + ': ' + $clone.text().trim() + '';
	};

	var insertButtons = function() {
		config.buttons.forEach(function(btnConfig) {

			$(btnConfig.container).each(function() {

				var $container = $(this);

				// Ignore adding the timeline config on a twitter /status/ url
				if (btnConfig.location) {
					if (btnConfig.location == "timeline" && window.location.pathname.indexOf("/status/") >= 0) return;
				}

				if (!!btnConfig.ignore) {
					if (btnConfig.ignore($container)) return;
				}

				if ($container.hasClass('buffer-inserted')) return;

				$container.addClass('buffer-inserted');

				var btn = btnConfig.create(btnConfig);

				if (btnConfig.after) $container.find(btnConfig.after).after(btn);
				else if (btnConfig.before) $container.find(btnConfig.before).before(btn);

				if (!!btnConfig.activator) btnConfig.activator(btn, btnConfig);

				var getData = btnConfig.data;
				var clearData = btnConfig.clear;

				var clearcb = function() {};

				$(btn).click(function(e) {
					e.preventDefault();

					if ($(this).hasClass('disabled'))
						return;

					const message = { data: getData(btn), action: "PUBLISH" }
					chrome.runtime.sendMessage(message);
				});
			});

		});

	};

	/**
	 * Remove extra buttons that are not needed or wanted
	 */
	var removeExtras = function() {
		$('.replies .buffer-tweet-button').remove();
		$('.inline-reply-tweetbox .buffer-tweet-button').remove();
	};

	var twitterLoop = function twitterLoop() {
		insertButtons();
		removeExtras();
		setTimeout(twitterLoop, 500);
	};

	// Add class for css scoping, try this twice in case the scripts load strangely
	var addBufferClass = function(argument) {
		document.body.classList.add('buffer-twitter');
		setTimeout(addBufferClass, 2000);
	}

	var start = function() {
		addBufferClass();
		twitterLoop();
	};

	var setAttributes = function(el, attrs) {
		for (var key in attrs) {
			el.setAttribute(key, attrs[key]);
		}
	}

	;(function check() {
		chrome.storage.local.get("buffer.op.twitter", function(result) {
			if (result["buffer.op.twitter"] === "twitter") {
				start();
			} else {
				setTimeout(check, 2000);
			}
		});
	}());
}());