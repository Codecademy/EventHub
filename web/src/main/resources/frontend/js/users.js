var Users = (function () {

  var cls = function () {

  };

  var USERS_PROPERTIES = {};
  var USER_KEYS;

  cls.render = function () {
    $('.body-container').html(Mustache.render(usersTemplate));

    var users;
    var params = $.deparam(window.location.search.substring(1));
    if (params.type === 'users') {
      users = params;
    }
    else {
      users = {};
      history.pushState("", document.title, window.location.pathname);
    }

    this.initializeFilters();
    this.bindInputListeners();
  };

  cls.initializeFilters = function () {
    var self = this;

    $.ajax({
      type: "GET",
      url: "/users/keys",
    }).done(function (keys) {
      USER_KEYS = JSON.parse(keys)
      self.renderFilterKey();
      self.bindAddFilterListener();
    });
  };

  cls.findUsers = function () {
    var self = this;

    var params = {
      ufk: [],
      ufv: []
    };

    $('.filters-container .filters').each(function (i, filters) {
      var $filterValue = $(filters).find('.filter-value--input');
      var $filterKey = $(filters).find('.filter-key--input');
      if ($filterValue.length) {
        params.ufk.push($filterKey.val());
        params.ufv.push($filterValue.val());
      }
    });

    $.ajax({
      type: "GET",
      url: "/users/find",
      data: params
    }).done(function(users) {
      users = JSON.parse(users);
      users.forEach(function (user, i) {
        USERS_PROPERTIES[user.external_user_id] = user;
      });
      self.renderFilteredUsers(users);
    });
  };

  cls.getUser = function(externalUserId) {
    var self = this;

    var params = {
      external_user_id: externalUserId,
      offset: 0,
      num_records: 100000
    };

    $.ajax({
      type: "GET",
      url: "/users/timeline",
      data: params
    }).done(function(timeline) {
      timeline = JSON.parse(timeline);
      self.renderUserTimeline(timeline, externalUserId);
    });
  };

  cls.bindInputListeners = function () {
    var self = this;
    $('.find-users').off().click(function () {
      $('.user-filters .spinner').addClass('rendered');
      self.findUsers();
    });
  };

  cls.bindAddFilterListener = function () {
    var self = this;
    $('.user-filters .add-filter').click(function () {
      self.renderFilterKey();
    });
  };

  cls.renderFilterKey = function () {
    var $filtersContainer = $('.user-filters .filters-container');
    $filtersContainer.append(Mustache.render(filterKeyTemplate));

    var $filterKey = $filtersContainer.find('.filter-key--input').last();

    $filterKey.typeahead({
      source: USER_KEYS,
      items: 10000
    });

    this.bindFilterKeyListeners($filterKey);
    this.bindRemoveFilterListener($filtersContainer)
  };

  cls.bindRemoveFilterListener = function ($filtersContainer) {
    var self = this;
    $filtersContainer.find('.remove-filter').last().click(function () {
      var $filters = $(this).parent();
      $filters.remove();
    });
  };

  cls.bindFilterKeyListeners = function ($filterKey) {
    var self = this;
    $filterKey.change(function () {
      $(this).parent().find('.filter-value').remove();
      self.renderFilterValue($(this));
    });
  };

  cls.bindUsersTableInputs = function () {
    var self = this;
    $('.users-table tr').click(function () {
      $('.users-table tr.active').removeClass('active');
      $(this).addClass('active');
      $('.user-show').addClass('rendered');
      var externalUserId = $(this).data('user');
      self.getUser(externalUserId);
    });
  };

  cls.renderFilterValue = function ($filterKey) {
    var $filters = $filterKey.parent();

    $filters.append(Mustache.render(filterValueTemplate));

    var $filterValue = $filters.find('.filter-value--input');

    var params = {
      user_key: $filterKey.val(),
    };

    $.ajax({
      type: "GET",
      url: "/users/values?" + $.param(params)
    }).done(function(values) {
      values = JSON.parse(values);

      $filterValue.typeahead({
        source: values,
        items: 10000
      });
    });
  };

  cls.renderFilteredUsers = function (users) {
    var table = [];

    users.forEach(function (user, i) {
      table.push({ index: i, user: user.external_user_id });
    });

    var view = {
      table: table
    }

    $('.table-container').html(Mustache.render(usersTableTemplate, view))

    this.bindUsersTableInputs();

    $('.users-table').addClass('rendered');
    $('.user-filters .spinner').removeClass('rendered');
  };

  cls.renderUserTimeline = function (timeline, externalUserId) {
    var properties = [];
    var userProperties = USERS_PROPERTIES[externalUserId];

    Object.keys(userProperties).forEach(function (key) {
      properties.push({ propertyName: key, propertyValue: userProperties[key] });
    });

    var lastDate;
    timeline = timeline.map(function (event) {
      var date = lastDate === event.date ? '' : Utils.unFormatDate(event.date)
      lastDate = event.date;
      return {
        date: date,
        event_type: event.event_type,
      }
    });

    var view = {
      timeline: timeline,
      properties: properties
    }

    $('.user-show-container').html(Mustache.render(userShowTemplate, view));
  };

  return cls;

});
