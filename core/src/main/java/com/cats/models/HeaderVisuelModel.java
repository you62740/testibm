package com.cats.models;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.Via;

import com.cats.constants.ModelConstants;

/**
 * Model associé au composant C241 - Header Visuel.
 * 
 * @author ETP7307
 *
 */
@Model(adaptables = { SlingHttpServletRequest.class })
public class HeaderVisuelModel {

	@Inject
	@Default(values = ModelConstants.DEFAULT_JCR_NULL_VALUE_STRING)
	@Named("imageFond")
	@Source(ModelConstants.VALUE_MAP_SOURCE)
	@Via(ModelConstants.VIA_RESOURCE)
	private String imageFond;

	@Inject
	@Default(values = ModelConstants.DEFAULT_JCR_NULL_VALUE_STRING)
	@Named("radioFormatAffichage")
	@Source(ModelConstants.VALUE_MAP_SOURCE)
	@Via(ModelConstants.VIA_RESOURCE)
	private String formatAffichage;

	@Inject
	@Default(values = ModelConstants.DEFAULT_JCR_NULL_VALUE_STRING)
	@Named("texteBouton")
	@Source(ModelConstants.VALUE_MAP_SOURCE)
	@Via(ModelConstants.VIA_RESOURCE)
	private String texteBouton;

	@Inject
	@Default(values = ModelConstants.DEFAULT_JCR_NULL_VALUE_STRING)
	@Named("urlBouton")
	@Source(ModelConstants.VALUE_MAP_SOURCE)
	@Via(ModelConstants.VIA_RESOURCE)
	private String urlBouton;

	@Inject
	@Default(values = ModelConstants.DEFAULT_JCR_NULL_VALUE_STRING)
	@Named("radioTypeBouton")
	@Source(ModelConstants.VALUE_MAP_SOURCE)
	@Via(ModelConstants.VIA_RESOURCE)
	private String typeBouton;

	/**
	 * @return le imageFond
	 */
	public String getImageFond() {
		return imageFond;
	}

	/**
	 * @return le formatAffichage
	 */
	public String getFormatAffichage() {
		return formatAffichage;
	}

	/**
	 * @return le texteBouton
	 */
	public String getTexteBouton() {
		return texteBouton;
	}

	/**
	 * @return le urlBouton
	 */
	public String getUrlBouton() {
		return urlBouton;
	}

	/**
	 * @return le typeBouton
	 */
	public String getTypeBouton() {
		return typeBouton;
	}

}