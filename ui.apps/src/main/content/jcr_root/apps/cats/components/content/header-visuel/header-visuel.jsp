<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>

       
<sling:adaptTo adaptable="${slingRequest}" adaptTo="com.cats.models.HeaderVisuelModel" var="headerVisuelModel" />

<c:if test="${headerVisuelModel.formatAffichage == 'flyer'}">
	<div class="HeaderVisuel">
</c:if>

<c:if test="${headerVisuelModel.formatAffichage == 'habillage'}">
	<div class="HeaderVisuel HeaderVisuel--habillage">
</c:if>


<div class="HeaderVisuel-background" style="background-image:url(${headerVisuelModel.imageFond})">
	<img src="${headerVisuelModel.imageFond}" alt="ALT TXT" class="HeaderVisuel-image">
</div>
<a class="HeaderVisuel-button" href="${headerVisuelModel.urlBouton}">${headerVisuelModel.texteBouton}</a>
</div>