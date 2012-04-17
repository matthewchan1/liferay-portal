/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.service.impl;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.io.DummyWriter;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.plugin.PluginPackage;
import com.liferay.portal.kernel.template.Template;
import com.liferay.portal.kernel.template.TemplateContextType;
import com.liferay.portal.kernel.template.TemplateManager;
import com.liferay.portal.kernel.template.TemplateManagerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.LayoutTemplate;
import com.liferay.portal.model.LayoutTemplateConstants;
import com.liferay.portal.model.PluginSetting;
import com.liferay.portal.model.impl.LayoutTemplateImpl;
import com.liferay.portal.service.base.LayoutTemplateLocalServiceBaseImpl;
import com.liferay.portal.util.PropsValues;
import com.liferay.portlet.layoutconfiguration.util.velocity.InitColumnProcessor;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

/**
 * @author Ivica Cardic
 * @author Jorge Ferrer
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 */
public class LayoutTemplateLocalServiceImpl
	extends LayoutTemplateLocalServiceBaseImpl {

	public String getContent(
			String layoutTemplateId, boolean standard, String themeId)
		throws SystemException {

		LayoutTemplate layoutTemplate = getLayoutTemplate(
			layoutTemplateId, standard, themeId);

		if (layoutTemplate == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Layout template " + layoutTemplateId + " does not exist");
			}

			layoutTemplate = getLayoutTemplate(
				PropsValues.DEFAULT_LAYOUT_TEMPLATE_ID, standard, themeId);

			if (layoutTemplate == null) {
				_log.error(
					"Layout template " + layoutTemplateId +
						" and default layout template " +
							PropsValues.DEFAULT_LAYOUT_TEMPLATE_ID +
								" do not exist");

				return StringPool.BLANK;
			}
		}

		if (PropsValues.LAYOUT_TEMPLATE_CACHE_ENABLED) {
			return layoutTemplate.getContent();
		}
		else {
			try {
				return layoutTemplate.getUncachedContent();
			}
			catch (IOException ioe) {
				throw new SystemException(ioe);
			}
		}
	}

	public LayoutTemplate getLayoutTemplate(
		String layoutTemplateId, boolean standard, String themeId) {

		if (Validator.isNull(layoutTemplateId)) {
			return null;
		}

		LayoutTemplate layoutTemplate = null;

		if (themeId != null) {
			if (standard) {
				layoutTemplate = _getThemesStandard(themeId).get(
					layoutTemplateId);
			}
			else {
				layoutTemplate = _getThemesCustom(themeId).get(
					layoutTemplateId);
			}

			if (layoutTemplate != null) {
				return layoutTemplate;
			}
		}

		if (standard) {
			layoutTemplate = _warStandard.get(layoutTemplateId);

			if (layoutTemplate == null) {
				layoutTemplate = _portalStandard.get(layoutTemplateId);
			}
		}
		else {
			layoutTemplate = _warCustom.get(layoutTemplateId);

			if (layoutTemplate == null) {
				layoutTemplate = _portalCustom.get(layoutTemplateId);
			}
		}

		return layoutTemplate;
	}

	public List<LayoutTemplate> getLayoutTemplates() {
		List<LayoutTemplate> customLayoutTemplates =
			new ArrayList<LayoutTemplate>(
							_portalCustom.size() + _warCustom.size());

		customLayoutTemplates.addAll(ListUtil.fromMapValues(_portalCustom));
		customLayoutTemplates.addAll(ListUtil.fromMapValues(_warCustom));

		return customLayoutTemplates;
	}

	public List<LayoutTemplate> getLayoutTemplates(String themeId) {
		Map<String, LayoutTemplate> _themesCustom = _getThemesCustom(themeId);

		List<LayoutTemplate> customLayoutTemplates =
			new ArrayList<LayoutTemplate>(
				_portalCustom.size() + _warCustom.size() +
					_themesCustom.size());

		for (Map.Entry<String, LayoutTemplate> entry :
				_portalCustom.entrySet()) {

			String layoutTemplateId = entry.getKey();
			LayoutTemplate layoutTemplate = entry.getValue();

			LayoutTemplate themeCustomLayoutTemplate = _themesCustom.get(
				layoutTemplateId);

			if (themeCustomLayoutTemplate != null) {
				customLayoutTemplates.add(themeCustomLayoutTemplate);
			}
			else {
				LayoutTemplate warCustomLayoutTemplate = _warCustom.get(
					layoutTemplateId);

				if (warCustomLayoutTemplate != null) {
					customLayoutTemplates.add(warCustomLayoutTemplate);
				}
				else {
					customLayoutTemplates.add(layoutTemplate);
				}
			}
		}

		for (Map.Entry<String, LayoutTemplate> entry : _warCustom.entrySet()) {
			String layoutTemplateId = entry.getKey();

			if (!_portalCustom.containsKey(layoutTemplateId) &&
				!_themesCustom.containsKey(layoutTemplateId)) {

				customLayoutTemplates.add(_warCustom.get(layoutTemplateId));
			}
		}

		for (Map.Entry<String, LayoutTemplate> entry :
				_themesCustom.entrySet()) {

			String layoutTemplateId = entry.getKey();

			if (!_portalCustom.containsKey(layoutTemplateId) &&
				!_warCustom.containsKey(layoutTemplateId)) {

				customLayoutTemplates.add(_themesCustom.get(layoutTemplateId));
			}
		}

		return customLayoutTemplates;
	}

	public String getWapContent(
			String layoutTemplateId, boolean standard, String themeId)
		throws SystemException {

		LayoutTemplate layoutTemplate = getLayoutTemplate(
			layoutTemplateId, standard, themeId);

		if (layoutTemplate == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Layout template " + layoutTemplateId + " does not exist");
			}

			layoutTemplate = getLayoutTemplate(
				PropsValues.DEFAULT_LAYOUT_TEMPLATE_ID, standard, themeId);

			if (layoutTemplate == null) {
				_log.error(
					"Layout template " + layoutTemplateId +
						" and default layout template " +
							PropsValues.DEFAULT_LAYOUT_TEMPLATE_ID +
								" do not exist");

				return StringPool.BLANK;
			}
		}

		if (PropsValues.LAYOUT_TEMPLATE_CACHE_ENABLED) {
			return layoutTemplate.getWapContent();
		}
		else {
			try {
				return layoutTemplate.getUncachedWapContent();
			}
			catch (IOException ioe) {
				throw new SystemException(ioe);
			}
		}
	}

	public List<ObjectValuePair<String, Boolean>> init(
		ServletContext servletContext, String[] xmls,
		PluginPackage pluginPackage) {

		return init(null, servletContext, xmls, pluginPackage);
	}

	public List<ObjectValuePair<String, Boolean>> init(
		String servletContextName, ServletContext servletContext, String[] xmls,
		PluginPackage pluginPackage) {

		List<ObjectValuePair<String, Boolean>> layoutTemplateIds =
			new ArrayList<ObjectValuePair<String, Boolean>>();

		try {
			for (int i = 0; i < xmls.length; i++) {
				Set<ObjectValuePair<String, Boolean>> curLayoutTemplateIds =
					_readLayoutTemplates(
						servletContextName, servletContext, xmls[i],
						pluginPackage);

				for (ObjectValuePair<String, Boolean> ovp :
						curLayoutTemplateIds) {

					if (!layoutTemplateIds.contains(ovp)) {
						layoutTemplateIds.add(ovp);
					}
				}
			}
		}
		catch (Exception e) {
			_log.error(e, e);
		}

		return layoutTemplateIds;
	}

	public void readLayoutTemplate(
		String servletContextName, ServletContext servletContext,
		Set<ObjectValuePair<String, Boolean>> layoutTemplateIds,
		Element element, boolean standard, String themeId,
		PluginPackage pluginPackage) {

		Map<String, LayoutTemplate> layoutTemplates = null;

		if (themeId != null) {
			if (standard) {
				layoutTemplates = _getThemesStandard(themeId);
			}
			else {
				layoutTemplates = _getThemesCustom(themeId);
			}
		}
		else if (servletContextName != null) {
			if (standard) {
				layoutTemplates = _warStandard;
			}
			else {
				layoutTemplates = _warCustom;
			}
		}
		else {
			if (standard) {
				layoutTemplates = _portalStandard;
			}
			else {
				layoutTemplates = _portalCustom;
			}
		}

		List<Element> layoutTemplateElements = element.elements(
						"layout-template");

		for (Element layoutTemplateElement : layoutTemplateElements) {
			String layoutTemplateId = layoutTemplateElement.attributeValue(
				"id");

			if (layoutTemplateIds != null) {
				ObjectValuePair<String, Boolean> ovp =
					new ObjectValuePair<String, Boolean>(
						layoutTemplateId, standard);

				layoutTemplateIds.add(ovp);
			}

			LayoutTemplate layoutTemplateModel = layoutTemplates.get(
				layoutTemplateId);

			if (layoutTemplateModel == null) {
				layoutTemplateModel = new LayoutTemplateImpl(layoutTemplateId);

				layoutTemplates.put(layoutTemplateId, layoutTemplateModel);
			}

			PluginSetting pluginSetting =
				pluginSettingLocalService.getDefaultPluginSetting();

			layoutTemplateModel.setPluginPackage(pluginPackage);
			layoutTemplateModel.setServletContext(servletContext);

			if (servletContextName != null) {
				layoutTemplateModel.setServletContextName(servletContextName);
			}

			layoutTemplateModel.setStandard(standard);
			layoutTemplateModel.setThemeId(themeId);
			layoutTemplateModel.setName(GetterUtil.getString(
				layoutTemplateElement.attributeValue("name"),
				layoutTemplateModel.getName()));
			layoutTemplateModel.setTemplatePath(GetterUtil.getString(
				layoutTemplateElement.elementText("template-path"),
				layoutTemplateModel.getTemplatePath()));
			layoutTemplateModel.setWapTemplatePath(GetterUtil.getString(
				layoutTemplateElement.elementText("wap-template-path"),
				layoutTemplateModel.getWapTemplatePath()));
			layoutTemplateModel.setThumbnailPath(GetterUtil.getString(
				layoutTemplateElement.elementText("thumbnail-path"),
				layoutTemplateModel.getThumbnailPath()));

			String content = null;

			try {
				content = HttpUtil.URLtoString(servletContext.getResource(
					layoutTemplateModel.getTemplatePath()));
			}
			catch (Exception e) {
				_log.error(
					"Unable to get content at template path " +
						layoutTemplateModel.getTemplatePath() + ": " +
							e.getMessage());
			}

			if (Validator.isNull(content)) {
				_log.error(
					"No content found at template path " +
						layoutTemplateModel.getTemplatePath());
			}
			else {
				StringBundler sb = new StringBundler(3);

				sb.append(themeId);

				if (standard) {
					sb.append(LayoutTemplateConstants.STANDARD_SEPARATOR);
				}
				else {
					sb.append(LayoutTemplateConstants.CUSTOM_SEPARATOR);
				}

				sb.append(layoutTemplateId);

				String velocityTemplateId = sb.toString();

				layoutTemplateModel.setContent(content);
				layoutTemplateModel.setColumns(
					_getColumns(velocityTemplateId, content));
			}

			if (Validator.isNull(layoutTemplateModel.getWapTemplatePath())) {
				_log.error(
					"The element wap-template-path is not defined for " +
						layoutTemplateId);
			}
			else {
				String wapContent = null;

				try {
					wapContent = HttpUtil.URLtoString(
						servletContext.getResource(
							layoutTemplateModel.getWapTemplatePath()));
				}
				catch (Exception e) {
					_log.error(
						"Unable to get content at WAP template path " +
							layoutTemplateModel.getWapTemplatePath() + ": " +
								e.getMessage());
				}

				if (Validator.isNull(wapContent)) {
					_log.error(
						"No content found at WAP template path " +
							layoutTemplateModel.getWapTemplatePath());
				}
				else {
					layoutTemplateModel.setWapContent(wapContent);
				}
			}

			Element rolesElement = layoutTemplateElement.element("roles");

			if (rolesElement != null) {
				List<Element> roleNameElements = rolesElement.elements(
					"role-name");

				for (Element roleNameElement : roleNameElements) {
					pluginSetting.addRole(roleNameElement.getText());
				}
			}

			layoutTemplateModel.setDefaultPluginSetting(pluginSetting);
		}
	}

	public void uninstallLayoutTemplate(
		String layoutTemplateId, boolean standard) {

		String templateId = null;

		try {
			if (standard) {
				templateId =
					"null" + LayoutTemplateConstants.STANDARD_SEPARATOR +
						layoutTemplateId;

				TemplateManagerUtil.clearCache(
					TemplateManager.VELOCITY, templateId);

				_warStandard.remove(layoutTemplateId);
			}
			else {
				templateId =
					"null" + LayoutTemplateConstants.CUSTOM_SEPARATOR +
						layoutTemplateId;

				TemplateManagerUtil.clearCache(
					TemplateManager.VELOCITY, templateId);

				_warCustom.remove(layoutTemplateId);
			}
		}
		catch (Exception e) {
			_log.error("Unable to uninstall layout template " + templateId, e);
		}
	}

	public void uninstallLayoutTemplates(String themeId) {
		Map<String, LayoutTemplate> _themesStandard = _getThemesStandard(
			themeId);

		for (Map.Entry<String, LayoutTemplate> entry :
				_themesStandard.entrySet()) {

			LayoutTemplate layoutTemplate = entry.getValue();

			String templateId =
				themeId + LayoutTemplateConstants.STANDARD_SEPARATOR +
					layoutTemplate.getLayoutTemplateId();

			try {
				TemplateManagerUtil.clearCache(
					TemplateManager.VELOCITY, templateId);
			}
			catch (Exception e) {
				_log.error(
					"Unable to uninstall layout template " + templateId, e);
			}
		}

		_themesStandard.clear();

		Map<String, LayoutTemplate> _themesCustom = _getThemesCustom(themeId);

		for (Map.Entry<String, LayoutTemplate> entry :
				_themesCustom.entrySet()) {

			LayoutTemplate layoutTemplate = entry.getValue();

			String templateId =
				themeId + LayoutTemplateConstants.CUSTOM_SEPARATOR +
					layoutTemplate.getLayoutTemplateId();

			try {
				TemplateManagerUtil.clearCache(
					TemplateManager.VELOCITY, templateId);
			}
			catch (Exception e) {
				_log.error(
					"Unable to uninstall layout template " + templateId, e);
			}
		}

		_themesCustom.clear();
	}

	private List<String> _getColumns(
		String velocityTemplateId, String velocityTemplateContent) {

		try {
			InitColumnProcessor processor = new InitColumnProcessor();

			Template velocityTemplate = TemplateManagerUtil.getTemplate(
				TemplateManager.VELOCITY, velocityTemplateId,
				velocityTemplateContent, TemplateContextType.STANDARD);

			velocityTemplate.put("processor", processor);

			velocityTemplate.processTemplate(new DummyWriter());

			return ListUtil.sort(processor.getColumns());
		}
		catch (Exception e) {
			_log.error(e);

			return new ArrayList<String>();
		}
	}

	private Map<String, LayoutTemplate> _getThemesCustom(String themeId) {
		String key = themeId.concat(LayoutTemplateConstants.CUSTOM_SEPARATOR);

		Map<String, LayoutTemplate> layoutTemplates = _themes.get(key);

		if (layoutTemplates == null) {
			layoutTemplates = new LinkedHashMap<String, LayoutTemplate>();

			_themes.put(key, layoutTemplates);
		}

		return layoutTemplates;
	}

	private Map<String, LayoutTemplate> _getThemesStandard(String themeId) {
		String key = themeId + LayoutTemplateConstants.STANDARD_SEPARATOR;

		Map<String, LayoutTemplate> layoutTemplates = _themes.get(key);

		if (layoutTemplates == null) {
			layoutTemplates = new LinkedHashMap<String, LayoutTemplate>();

			_themes.put(key, layoutTemplates);
		}

		return layoutTemplates;
	}

	private Set<ObjectValuePair<String, Boolean>> _readLayoutTemplates(
			String servletContextName, ServletContext servletContext,
			String xml, PluginPackage pluginPackage)
		throws Exception {

		Set<ObjectValuePair<String, Boolean>> layoutTemplateIds =
			new HashSet<ObjectValuePair<String, Boolean>>();

		if (xml == null) {
			return layoutTemplateIds;
		}

		Document document = SAXReaderUtil.read(xml, true);

		Element rootElement = document.getRootElement();

		Element standardElement = rootElement.element("standard");

		if (standardElement != null) {
			readLayoutTemplate(
				servletContextName, servletContext, layoutTemplateIds,
				standardElement, true, null, pluginPackage);
		}

		Element customElement = rootElement.element("custom");

		if (customElement != null) {
			readLayoutTemplate(
				servletContextName, servletContext, layoutTemplateIds,
				customElement, false, null, pluginPackage);
		}

		return layoutTemplateIds;
	}

	private static Log _log = LogFactoryUtil.getLog(
		LayoutTemplateLocalServiceImpl.class);

	private static Map<String, LayoutTemplate> _portalCustom =
		new LinkedHashMap<String, LayoutTemplate>();
	private static Map<String, LayoutTemplate> _portalStandard =
		new LinkedHashMap<String, LayoutTemplate>();

	private static Map<String, Map<String, LayoutTemplate>> _themes =
		new LinkedHashMap<String, Map<String, LayoutTemplate>>();

	private static Map<String, LayoutTemplate> _warCustom =
		new LinkedHashMap<String, LayoutTemplate>();
	private static Map<String, LayoutTemplate> _warStandard =
		new LinkedHashMap<String, LayoutTemplate>();

}