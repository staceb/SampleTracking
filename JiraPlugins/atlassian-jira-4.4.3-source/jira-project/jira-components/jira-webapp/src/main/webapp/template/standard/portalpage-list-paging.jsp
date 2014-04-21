<%@ taglib uri="webwork" prefix="ww" %>
<ww:if test="parameters['portalPageList'] != null && parameters['portalPageList']/size > 0 && parameters['paging'] != null && parameters['paging'] == true">
    <ww:if test="parameters['pagingNextUrl'] != null || parameters['pagingPrevUrl'] != null">
        <p class="pagination">
            <ww:if test="parameters['pagingPrevUrl'] != null">
                <a class="icon icon-previous" href="<ww:property value="parameters['pagingPrevUrl']"/>"><span><ww:text name="'common.forms.previous.with.arrows'"/></span></a>
            </ww:if>
            <ww:if test="parameters['pagingPrevUrl'] == null && parameters['pagingNextUrl'] == null">
                &nbsp;
            </ww:if>
            <ww:if test="parameters['pagingMessage'] != null">
                <span><ww:property value="parameters['pagingMessage']"/></span>
            </ww:if>
            <ww:if test="parameters['pagingNextUrl'] != null">
                <a class="icon icon-next" href="<ww:property value="parameters['pagingNextUrl']"/>"><span><ww:text name="'common.forms.next.with.arrows'"/></span></a>
            </ww:if>
        </p>
    </ww:if>
</ww:if> 