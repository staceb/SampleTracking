(function() {
    function createPicker($selectField) {
        new AJS.MultiSelect({
           element: $selectField,
           itemAttrDisplayed: "label",
           errorMessage: AJS.I18n.getText("jira.ajax.autocomplete.versions.error"),
           maxInlineResultsDisplayed: 15
        });
    }

    function locateSelect(parent) {
        var $parent = AJS.$(parent),
            $selectField;

        if ($parent.is("select")) {
            $selectField = $parent;
        } else {
            $selectField = $parent.find("select");
        }

        return $selectField;
    }

    var DEFAULT_SELECTORS = [
        "div.aui-field-versionspicker.frother-control-renderer", // aui forms
        "td.aui-field-versionspicker.frother-control-renderer", // convert to subtask and move
        "tr.aui-field-versionspicker.frother-control-renderer" // bulk edit
    ];

    function findVersionSelectAndConvertToPicker(context, selector) {
        selector = selector || DEFAULT_SELECTORS.join(", ");

        AJS.$(selector, context).each(function () {

            var $selectField = locateSelect(this);

            if ($selectField.length) {
                createPicker($selectField);
            }

        });
    }

    AJS.$(function() {
        findVersionSelectAndConvertToPicker();
    });

    AJS.$(document).bind("dialogContentReady", function(e, dialog) {
        findVersionSelectAndConvertToPicker(dialog.get$popupContent());
    });
})();
