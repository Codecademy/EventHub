var Users = (function () {

  var cls = function () {

  };

  cls.render = function () {
    $('.body-container').html(Mustache.render(usersTemplate));

    var params = $.deparam(window.location.search.substring(1));
    var retention = params.type === 'users' ? params : {};

    this.renderFilterKey();
    this.bindAddFilterListener();
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
      source: ['no filter'].concat(['1','2','3']),
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
      if ($(this).val() !== 'no filter') {
        self.renderFilterValue($(this));
      }
    });
  };

  cls.renderFilterValue = function ($filterKey) {
    var $filters = $filterKey.parent();

    $filters.append(Mustache.render(filterValueTemplate));

    var $filterValue = $filters.find('.filter-value--input');

    $filterValue.typeahead({
      source: ['1','2','3','4'],
      items: 10000
    });
  };

  return cls;

});
