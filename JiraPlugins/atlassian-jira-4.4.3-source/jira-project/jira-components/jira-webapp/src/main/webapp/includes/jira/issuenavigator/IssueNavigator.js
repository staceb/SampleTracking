/**
 * Represents an the Issue Navigator page.  This class should be used to retrieve information from the
 * issue navigator such as the currently selected row, currently selected issue key and so on.
 *
 * @namespace JIRA.IssueNavigator
 */
JIRA.IssueNavigator = {
    /**
     * Checks if we are currently viewing the issue navigator.
     *
     * @method isNavigator
     * @return {Boolean} true if the current page is the issue navigator, false otherwise
     */
    isNavigator: function() {
        return jQuery("#isNavigator").length === 1;
    },

    /**
     * Checks if any row is currently selected on the issue navigator. This can be the case for
     * an empty searchr, or if keyboard shortcuts are disabled.
     *
     * @method isRowSelected
     * @return {Boolean} true if a selected issue row exists, false otherwise
     */
    isRowSelected: function() {
        return JIRA.IssueNavigator.get$focusedRow().length !== 0;
    },

    /**
     * Returns a jQuery wrapped object representing the currently selected issue row.
     *
     * @method getFocusedRow
     * @return {jQuery} the jQuery wrapped issue row representing the currently selected row
     */
    get$focusedRow: function() {
        return jQuery("#issuetable tr.issuerow.focused");
    },

    /**
     * Gets the index of the focused issue.
     *
     * @method getFocsuedIssueIndex
     * @return {Number} - The index of the focused issue in the current search result set.
     */
    getFocsuedIssueIndex: function() {
        var rowIndex = jQuery("#issuetable").find("tr.issuerow").index(JIRA.IssueNavigator.get$focusedRow());
        var searchOffset = parseInt(jQuery('#results-count-start').text(), 10) - 1;
        return rowIndex + searchOffset;
    },

    /**
     * Returns the issue key for the currently selected row.
     *
     * @method getSelectedIssueKey
     * @return {String} The issue key for the currently focused row or undefined if none exists.
     */
    getSelectedIssueKey: function() {
        var $focusedRow = JIRA.IssueNavigator.get$focusedRow();
        if ($focusedRow.length !== 0) {
            return $focusedRow.attr("data-issuekey");
        }
        return undefined;
    },

    /**
     * Returns the issue id for the currently selected row.
     *
     * @method getSelectedIssueId
     * @return {String} The issue id for the currently focused row or undefined if none exists.
     */
    getSelectedIssueId: function() {
        return JIRA.IssueNavigator.get$focusedRow().attr("rel");
    },

    /**
     * Returns the issue id for the next row after the currently selected row.
     *
     * Note: It is a known issue that no id will be returned when the last issue on the page is
     * focused. In future, the return value in this situation may change.
     *
     * @method getNextIssueId
     * @return {String} The issue id for the next issue after the currently focused row or undefined if none exists.
     */
    getNextIssueId: function() {
        return JIRA.IssueNavigator.get$focusedRow().next("tr.issuerow").attr("rel");
    }
};

/** Preserve legacy namespace
    @deprecated jira.app.issuenavigator */
AJS.namespace("jira.app.issuenavigator", null, JIRA.IssueNavigator);
