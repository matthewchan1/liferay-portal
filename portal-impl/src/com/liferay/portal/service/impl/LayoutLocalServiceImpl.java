/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

import com.liferay.exportimport.kernel.staging.MergeLayoutPrototypesThreadLocal;
import com.liferay.layouts.admin.kernel.model.LayoutTypePortletConstants;
import com.liferay.petra.lang.CentralizedThreadLocal;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.NoSuchLayoutException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.RequiredLayoutException;
import com.liferay.portal.kernel.exception.SitemapChangeFrequencyException;
import com.liferay.portal.kernel.exception.SitemapIncludeException;
import com.liferay.portal.kernel.exception.SitemapPagePriorityException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.CustomizedPages;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.LayoutFriendlyURL;
import com.liferay.portal.kernel.model.LayoutPrototype;
import com.liferay.portal.kernel.model.LayoutReference;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.LayoutSetPrototype;
import com.liferay.portal.kernel.model.LayoutType;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.LayoutVersion;
import com.liferay.portal.kernel.model.PortalPreferences;
import com.liferay.portal.kernel.model.PortletConstants;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.SystemEventConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.impl.VirtualLayout;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.version.VersionServiceListener;
import com.liferay.portal.kernel.systemevent.SystemEvent;
import com.liferay.portal.kernel.systemevent.SystemEventHierarchyEntry;
import com.liferay.portal.kernel.systemevent.SystemEventHierarchyEntryThreadLocal;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.comparator.LayoutComparator;
import com.liferay.portal.kernel.util.comparator.LayoutPriorityComparator;
import com.liferay.portal.kernel.workflow.WorkflowThreadLocal;
import com.liferay.portal.service.base.LayoutLocalServiceBaseImpl;
import com.liferay.portal.util.PropsValues;
import com.liferay.portlet.PortalPreferencesImpl;
import com.liferay.sites.kernel.util.Sites;
import com.liferay.sites.kernel.util.SitesUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Provides the local service for accessing, adding, deleting, exporting,
 * importing, and updating layouts.
 *
 * @author Brian Wing Shun Chan
 * @author Jorge Ferrer
 * @author Joel Kozikowski
 * @author Charles May
 * @author Raymond Augé
 * @author Jorge Ferrer
 * @author Bruno Farache
 * @author Vilmos Papp
 * @author James Lefeu
 * @author Tibor Lipusz
 */
public class LayoutLocalServiceImpl extends LayoutLocalServiceBaseImpl {

	/**
	 * Returns the object counter's name.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether layout is private to the group
	 * @return the object counter's name
	 */
	public static String getCounterName(long groupId, boolean privateLayout) {
		StringBundler sb = new StringBundler(5);

		sb.append(Layout.class.getName());
		sb.append(StringPool.POUND);
		sb.append(groupId);
		sb.append(StringPool.POUND);
		sb.append(privateLayout);

		return sb.toString();
	}

	/**
	 * Adds a layout with additional parameters.
	 *
	 * <p>
	 * This method handles the creation of the layout including its resources,
	 * metadata, and internal data structures. It is not necessary to make
	 * subsequent calls to any methods to setup default groups, resources, ...
	 * etc.
	 * </p>
	 *
	 * @param  userId the primary key of the user
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout (optionally
	 *         {@link LayoutConstants#DEFAULT_PARENT_LAYOUT_ID})
	 * @param  classNameId the class name ID of the entity
	 * @param  classPK the primary key of the entity
	 * @param  nameMap the layout's locales and localized names
	 * @param  titleMap the layout's locales and localized titles
	 * @param  descriptionMap the layout's locales and localized descriptions
	 * @param  keywordsMap the layout's locales and localized keywords
	 * @param  robotsMap the layout's locales and localized robots
	 * @param  type the layout's type (optionally {@link
	 *         LayoutConstants#TYPE_PORTLET}). The possible types can be found
	 *         in {@link LayoutConstants}.
	 * @param  typeSettings the settings to load the unicode properties object.
	 *         See {@link UnicodeProperties #fastLoad(String)}.
	 * @param  hidden whether the layout is hidden
	 * @param  system whether the layout is of system type
	 * @param  friendlyURLMap the layout's locales and localized friendly URLs.
	 *         To see how the URL is normalized when accessed, see {@link
	 *         com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil#normalize(
	 *         String)}.
	 * @param  serviceContext the service context to be applied. Must set the
	 *         UUID for the layout. Can set the creation date, modification
	 *         date, and expando bridge attributes for the layout. For layouts
	 *         that belong to a layout set prototype, an attribute named
	 *         <code>layoutUpdateable</code> can be set to specify whether site
	 *         administrators can modify this page within their site. For
	 *         layouts that are created from a layout prototype, attributes
	 *         named <code>layoutPrototypeUuid</code> and
	 *         <code>layoutPrototypeLinkedEnabled</code> can be specified to
	 *         provide the unique identifier of the source prototype and a
	 *         boolean to determine whether a link to it should be enabled to
	 *         activate propagation of changes made to the linked page in the
	 *         prototype.
	 * @return the layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public Layout addLayout(
			long userId, long groupId, boolean privateLayout,
			long parentLayoutId, long classNameId, long classPK,
			Map<Locale, String> nameMap, Map<Locale, String> titleMap,
			Map<Locale, String> descriptionMap, Map<Locale, String> keywordsMap,
			Map<Locale, String> robotsMap, String type, String typeSettings,
			boolean hidden, boolean system, Map<Locale, String> friendlyURLMap,
			ServiceContext serviceContext)
		throws PortalException {

		// Layout

		User user = userPersistence.findByPrimaryKey(userId);
		long layoutId = getNextLayoutId(groupId, privateLayout);
		parentLayoutId = layoutLocalServiceHelper.getParentLayoutId(
			groupId, privateLayout, parentLayoutId);

		String name = nameMap.get(LocaleUtil.getSiteDefault());

		friendlyURLMap = layoutLocalServiceHelper.getFriendlyURLMap(
			groupId, privateLayout, layoutId, name, friendlyURLMap);

		String friendlyURL = friendlyURLMap.get(LocaleUtil.getSiteDefault());

		UnicodeProperties typeSettingsProperties = new UnicodeProperties();

		typeSettingsProperties.fastLoad(typeSettings);

		int priority = Integer.MAX_VALUE;

		if (!system) {
			priority = layoutLocalServiceHelper.getNextPriority(
				groupId, privateLayout, parentLayoutId, null, -1);
		}

		layoutLocalServiceHelper.validate(
			groupId, privateLayout, layoutId, parentLayoutId, name, type,
			hidden, friendlyURLMap, serviceContext);

		Date now = new Date();

		Layout draftLayout = create();

		String uuid = serviceContext.getUuid();

		if (Validator.isNotNull(uuid)) {
			draftLayout.setUuid(uuid);
		}

		draftLayout.setGroupId(groupId);
		draftLayout.setCompanyId(user.getCompanyId());
		draftLayout.setUserId(user.getUserId());
		draftLayout.setUserName(user.getFullName());
		draftLayout.setCreateDate(serviceContext.getCreateDate(now));
		draftLayout.setModifiedDate(serviceContext.getModifiedDate(now));
		draftLayout.setParentPlid(
			_getParentPlid(groupId, privateLayout, parentLayoutId));
		draftLayout.setPrivateLayout(privateLayout);
		draftLayout.setLayoutId(layoutId);
		draftLayout.setParentLayoutId(parentLayoutId);
		draftLayout.setClassNameId(classNameId);
		draftLayout.setClassPK(classPK);
		draftLayout.setNameMap(nameMap);
		draftLayout.setTitleMap(titleMap);
		draftLayout.setDescriptionMap(descriptionMap);
		draftLayout.setKeywordsMap(keywordsMap);
		draftLayout.setRobotsMap(robotsMap);
		draftLayout.setType(type);
		draftLayout.setHidden(hidden);
		draftLayout.setSystem(system);
		draftLayout.setFriendlyURL(friendlyURL);
		draftLayout.setPriority(priority);
		draftLayout.setPublishDate(serviceContext.getModifiedDate(now));

		boolean layoutUpdateable = ParamUtil.getBoolean(
			serviceContext, Sites.LAYOUT_UPDATEABLE, true);

		if (!layoutUpdateable) {
			typeSettingsProperties.put(
				Sites.LAYOUT_UPDATEABLE, String.valueOf(layoutUpdateable));
		}

		if (privateLayout) {
			typeSettingsProperties.put(
				"privateLayout", String.valueOf(privateLayout));
		}

		validateTypeSettingsProperties(draftLayout, typeSettingsProperties);

		draftLayout.setTypeSettingsProperties(typeSettingsProperties);

		String layoutPrototypeUuid = ParamUtil.getString(
			serviceContext, "layoutPrototypeUuid");
		boolean layoutPrototypeLinkEnabled = ParamUtil.getBoolean(
			serviceContext, "layoutPrototypeLinkEnabled",
			PropsValues.LAYOUT_PROTOTYPE_LINK_ENABLED_DEFAULT);

		if (Validator.isNotNull(layoutPrototypeUuid)) {
			draftLayout.setLayoutPrototypeUuid(layoutPrototypeUuid);
			draftLayout.setLayoutPrototypeLinkEnabled(
				layoutPrototypeLinkEnabled);
		}

		draftLayout.setExpandoBridgeAttributes(serviceContext);

		if (type.equals(LayoutConstants.TYPE_PORTLET)) {
			LayoutTypePortlet layoutTypePortlet =
				(LayoutTypePortlet)draftLayout.getLayoutType();

			if (Validator.isNull(layoutTypePortlet.getLayoutTemplateId())) {
				layoutTypePortlet.setLayoutTemplateId(
					0, PropsValues.LAYOUT_DEFAULT_TEMPLATE_ID, false);
			}
		}

		Layout layout = updateDraft(draftLayout);

		// Layout friendly URLs

		layoutFriendlyURLLocalService.updateLayoutFriendlyURLs(
			user.getUserId(), user.getCompanyId(), groupId, layout.getPlid(),
			privateLayout, friendlyURLMap, serviceContext);

		// Layout prototype

		if (Validator.isNotNull(layoutPrototypeUuid) &&
			!layoutPrototypeLinkEnabled) {

			_applyLayoutPrototype(
				layoutPrototypeUuid, layout, layoutPrototypeLinkEnabled);
		}

		// Resources

		boolean addGroupPermissions = true;

		Group group = groupLocalService.getGroup(groupId);

		if (privateLayout && (group.isUser() || group.isUserGroup())) {
			addGroupPermissions = false;
		}

		boolean addGuestPermissions = false;

		if (!privateLayout || type.equals(LayoutConstants.TYPE_CONTROL_PANEL) ||
			group.isLayoutSetPrototype()) {

			addGuestPermissions = true;
		}

		resourceLocalService.addResources(
			user.getCompanyId(), groupId, user.getUserId(),
			Layout.class.getName(), layout.getPlid(), false,
			addGroupPermissions, addGuestPermissions);

		// Group

		groupLocalService.updateSite(groupId, true);

		// Layout set

		layoutSetLocalService.updatePageCount(groupId, privateLayout);

		layout.setLayoutSet(null);

		// Asset

		updateAsset(
			userId, layout, serviceContext.getAssetCategoryIds(),
			serviceContext.getAssetTagNames());

		// Draft layout

		if (!system &&
			(Objects.equals(type, LayoutConstants.TYPE_CONTENT) ||
			 Objects.equals(type, LayoutConstants.TYPE_ASSET_DISPLAY))) {

			serviceContext.setModifiedDate(now);

			addLayout(
				userId, groupId, privateLayout, parentLayoutId,
				classNameLocalService.getClassNameId(Layout.class),
				layout.getPlid(), nameMap, titleMap, descriptionMap,
				keywordsMap, robotsMap, type, typeSettings, true, true,
				Collections.emptyMap(), serviceContext);
		}

		return layoutLocalService.getLayout(layout.getPlid());
	}

	/**
	 * Adds a layout with additional parameters.
	 *
	 * <p>
	 * This method handles the creation of the layout including its resources,
	 * metadata, and internal data structures. It is not necessary to make
	 * subsequent calls to any methods to setup default groups, resources, ...
	 * etc.
	 * </p>
	 *
	 * @param  userId the primary key of the user
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout (optionally
	 *         {@link LayoutConstants#DEFAULT_PARENT_LAYOUT_ID})
	 * @param  nameMap the layout's locales and localized names
	 * @param  titleMap the layout's locales and localized titles
	 * @param  descriptionMap the layout's locales and localized descriptions
	 * @param  keywordsMap the layout's locales and localized keywords
	 * @param  robotsMap the layout's locales and localized robots
	 * @param  type the layout's type (optionally {@link
	 *         LayoutConstants#TYPE_PORTLET}). The possible types can be found
	 *         in {@link LayoutConstants}.
	 * @param  typeSettings the settings to load the unicode properties object.
	 *         See {@link UnicodeProperties #fastLoad(String)}.
	 * @param  hidden whether the layout is hidden
	 * @param  system whether the layout is of system type
	 * @param  friendlyURLMap the layout's locales and localized friendly URLs.
	 *         To see how the URL is normalized when accessed, see {@link
	 *         com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil#normalize(
	 *         String)}.
	 * @param  serviceContext the service context to be applied. Must set the
	 *         UUID for the layout. Can set the creation date, modification
	 *         date, and expando bridge attributes for the layout. For layouts
	 *         that belong to a layout set prototype, an attribute named
	 *         <code>layoutUpdateable</code> can be set to specify whether site
	 *         administrators can modify this page within their site. For
	 *         layouts that are created from a layout prototype, attributes
	 *         named <code>layoutPrototypeUuid</code> and
	 *         <code>layoutPrototypeLinkedEnabled</code> can be specified to
	 *         provide the unique identifier of the source prototype and a
	 *         boolean to determine whether a link to it should be enabled to
	 *         activate propagation of changes made to the linked page in the
	 *         prototype.
	 * @return the layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public Layout addLayout(
			long userId, long groupId, boolean privateLayout,
			long parentLayoutId, Map<Locale, String> nameMap,
			Map<Locale, String> titleMap, Map<Locale, String> descriptionMap,
			Map<Locale, String> keywordsMap, Map<Locale, String> robotsMap,
			String type, String typeSettings, boolean hidden, boolean system,
			Map<Locale, String> friendlyURLMap, ServiceContext serviceContext)
		throws PortalException {

		return addLayout(
			userId, groupId, privateLayout, parentLayoutId, 0, 0, nameMap,
			titleMap, descriptionMap, keywordsMap, robotsMap, type,
			typeSettings, hidden, system, friendlyURLMap, serviceContext);
	}

	/**
	 * Adds a layout with additional parameters.
	 *
	 * <p>
	 * This method handles the creation of the layout including its resources,
	 * metadata, and internal data structures. It is not necessary to make
	 * subsequent calls to any methods to setup default groups, resources, ...
	 * etc.
	 * </p>
	 *
	 * @param  userId the primary key of the user
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout (optionally
	 *         {@link LayoutConstants#DEFAULT_PARENT_LAYOUT_ID})
	 * @param  nameMap the layout's locales and localized names
	 * @param  titleMap the layout's locales and localized titles
	 * @param  descriptionMap the layout's locales and localized descriptions
	 * @param  keywordsMap the layout's locales and localized keywords
	 * @param  robotsMap the layout's locales and localized robots
	 * @param  type the layout's type (optionally {@link
	 *         LayoutConstants#TYPE_PORTLET}). The possible types can be found
	 *         in {@link LayoutConstants}.
	 * @param  typeSettings the settings to load the unicode properties object.
	 *         See {@link UnicodeProperties #fastLoad(String)}.
	 * @param  hidden whether the layout is hidden
	 * @param  friendlyURLMap the layout's locales and localized friendly URLs.
	 *         To see how the URL is normalized when accessed, see {@link
	 *         com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil#normalize(
	 *         String)}.
	 * @param  serviceContext the service context to be applied. Must set the
	 *         UUID for the layout. Can set the creation date, modification
	 *         date, and expando bridge attributes for the layout. For layouts
	 *         that belong to a layout set prototype, an attribute named
	 *         <code>layoutUpdateable</code> can be set to specify whether site
	 *         administrators can modify this page within their site. For
	 *         layouts that are created from a layout prototype, attributes
	 *         named <code>layoutPrototypeUuid</code> and
	 *         <code>layoutPrototypeLinkedEnabled</code> can be specified to
	 *         provide the unique identifier of the source prototype and a
	 *         boolean to determine whether a link to it should be enabled to
	 *         activate propagation of changes made to the linked page in the
	 *         prototype.
	 * @return the layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public Layout addLayout(
			long userId, long groupId, boolean privateLayout,
			long parentLayoutId, Map<Locale, String> nameMap,
			Map<Locale, String> titleMap, Map<Locale, String> descriptionMap,
			Map<Locale, String> keywordsMap, Map<Locale, String> robotsMap,
			String type, String typeSettings, boolean hidden,
			Map<Locale, String> friendlyURLMap, ServiceContext serviceContext)
		throws PortalException {

		return addLayout(
			userId, groupId, privateLayout, parentLayoutId, nameMap, titleMap,
			descriptionMap, keywordsMap, robotsMap, type, typeSettings, hidden,
			false, friendlyURLMap, serviceContext);
	}

	/**
	 * Adds a layout with single entry maps for name, title, and description to
	 * the default locale.
	 *
	 * <p>
	 * This method handles the creation of the layout including its resources,
	 * metadata, and internal data structures. It is not necessary to make
	 * subsequent calls to any methods to setup default groups, resources, ...
	 * etc.
	 * </p>
	 *
	 * @param  userId the primary key of the user
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout (optionally
	 *         {@link LayoutConstants#DEFAULT_PARENT_LAYOUT_ID}). The possible
	 *         values can be found in {@link LayoutConstants}.
	 * @param  name the layout's name (optionally {@link
	 *         PropsValues#DEFAULT_USER_PRIVATE_LAYOUT_NAME} or {@link
	 *         PropsValues#DEFAULT_USER_PUBLIC_LAYOUT_NAME}). The default values
	 *         can be overridden in <code>portal-ext.properties</code> by
	 *         specifying new values for the corresponding properties defined in
	 *         {@link PropsValues}
	 * @param  title the layout's title
	 * @param  description the layout's description
	 * @param  type the layout's type (optionally {@link
	 *         LayoutConstants#TYPE_PORTLET}). The possible types can be found
	 *         in {@link LayoutConstants}.
	 * @param  hidden whether the layout is hidden
	 * @param  system whether the layout is of system type
	 * @param  friendlyURL the friendly URL of the layout (optionally {@link
	 *         PropsValues#DEFAULT_USER_PRIVATE_LAYOUT_FRIENDLY_URL} or {@link
	 *         PropsValues#DEFAULT_USER_PUBLIC_LAYOUT_FRIENDLY_URL}). The
	 *         default values can be overridden in
	 *         <code>portal-ext.properties</code> by specifying new values for
	 *         the corresponding properties defined in {@link PropsValues}. To
	 *         see how the URL is normalized when accessed, see {@link
	 *         com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil#normalize(
	 *         String)}.
	 * @param  serviceContext the service context to be applied. Must set the
	 *         UUID for the layout. Can set the creation date and modification
	 *         date for the layout. For layouts that belong to a layout set
	 *         prototype, an attribute named <code>layoutUpdateable</code> can
	 *         be set to specify whether site administrators can modify this
	 *         page within their site.
	 * @return the layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout addLayout(
			long userId, long groupId, boolean privateLayout,
			long parentLayoutId, String name, String title, String description,
			String type, boolean hidden, boolean system, String friendlyURL,
			ServiceContext serviceContext)
		throws PortalException {

		Locale locale = LocaleUtil.getSiteDefault();

		Map<Locale, String> nameMap = new HashMap<>();

		nameMap.put(locale, name);

		Map<Locale, String> titleMap = new HashMap<>();

		titleMap.put(locale, title);

		Map<Locale, String> descriptionMap = new HashMap<>();

		descriptionMap.put(locale, description);

		Map<Locale, String> friendlyURLMap = new HashMap<>();

		friendlyURLMap.put(LocaleUtil.getSiteDefault(), friendlyURL);

		return addLayout(
			userId, groupId, privateLayout, parentLayoutId, nameMap, titleMap,
			descriptionMap, new HashMap<Locale, String>(),
			new HashMap<Locale, String>(), type, StringPool.BLANK, hidden,
			system, friendlyURLMap, serviceContext);
	}

	/**
	 * Adds a layout with single entry maps for name, title, and description to
	 * the default locale.
	 *
	 * <p>
	 * This method handles the creation of the layout including its resources,
	 * metadata, and internal data structures. It is not necessary to make
	 * subsequent calls to any methods to setup default groups, resources, ...
	 * etc.
	 * </p>
	 *
	 * @param  userId the primary key of the user
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout (optionally
	 *         {@link LayoutConstants#DEFAULT_PARENT_LAYOUT_ID}). The possible
	 *         values can be found in {@link LayoutConstants}.
	 * @param  name the layout's name (optionally {@link
	 *         PropsValues#DEFAULT_USER_PRIVATE_LAYOUT_NAME} or {@link
	 *         PropsValues#DEFAULT_USER_PUBLIC_LAYOUT_NAME}). The default values
	 *         can be overridden in <code>portal-ext.properties</code> by
	 *         specifying new values for the corresponding properties defined in
	 *         {@link PropsValues}
	 * @param  title the layout's title
	 * @param  description the layout's description
	 * @param  type the layout's type (optionally {@link
	 *         LayoutConstants#TYPE_PORTLET}). The possible types can be found
	 *         in {@link LayoutConstants}.
	 * @param  hidden whether the layout is hidden
	 * @param  friendlyURL the friendly URL of the layout (optionally {@link
	 *         PropsValues#DEFAULT_USER_PRIVATE_LAYOUT_FRIENDLY_URL} or {@link
	 *         PropsValues#DEFAULT_USER_PUBLIC_LAYOUT_FRIENDLY_URL}). The
	 *         default values can be overridden in
	 *         <code>portal-ext.properties</code> by specifying new values for
	 *         the corresponding properties defined in {@link PropsValues}. To
	 *         see how the URL is normalized when accessed, see {@link
	 *         com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil#normalize(
	 *         String)}.
	 * @param  serviceContext the service context to be applied. Must set the
	 *         UUID for the layout. Can set the creation date and modification
	 *         date for the layout. For layouts that belong to a layout set
	 *         prototype, an attribute named <code>layoutUpdateable</code> can
	 *         be set to specify whether site administrators can modify this
	 *         page within their site.
	 * @return the layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout addLayout(
			long userId, long groupId, boolean privateLayout,
			long parentLayoutId, String name, String title, String description,
			String type, boolean hidden, String friendlyURL,
			ServiceContext serviceContext)
		throws PortalException {

		return addLayout(
			userId, groupId, privateLayout, parentLayoutId, name, title,
			description, type, hidden, false, friendlyURL, serviceContext);
	}

	/**
	 * Anonymize user information of the specific layout
	 *
	 * @param layout the layout that need to anonymized
	 * @param userId the primary key of the owner user
	 * @param anonymousUser the anonymized user information
	 */
	@Override
	public void anonymizeLayout(Layout layout, long userId, User anonymousUser)
		throws PortalException {

		if (layout == null) {
			throw new NoSuchLayoutException();
		}

		if (layout.getUserId() != userId) {
			return;
		}

		List<LayoutVersion> layoutVersions =
			layoutVersionPersistence.findByPlid(layout.getPlid());

		Stream<LayoutVersion> layoutVersionStream = layoutVersions.stream();

		layoutVersionStream.forEach(
			layoutVersion -> {
				if (layoutVersion.getUserId() == userId) {
					layoutVersion = layoutVersionPersistence.remove(
						layoutVersion);

					layoutVersion.setNew(true);
					layoutVersion.setUserId(anonymousUser.getUserId());
					layoutVersion.setUserName(anonymousUser.getFullName());

					layoutVersionPersistence.update(layoutVersion);
				}
			});

		layout.setUserId(anonymousUser.getUserId());
		layout.setUserName(anonymousUser.getFullName());

		layoutPersistence.update(layout);
	}

	@Override
	public Layout checkout(Layout layout, int version) throws PortalException {
		throw new UnsupportedOperationException();
	}

	@Indexable(type = IndexableType.DELETE)
	@Override
	public Layout delete(Layout layout) throws PortalException {
		return layoutPersistence.remove(layout);
	}

	@Indexable(type = IndexableType.DELETE)
	@Override
	public Layout deleteDraft(Layout layout) throws PortalException {
		return layoutPersistence.remove(layout);
	}

	/**
	 * Deletes the layout, its child layouts, and its associated resources.
	 *
	 * @param  layout the layout
	 * @return the deleted layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout deleteLayout(Layout layout) throws PortalException {
		layoutLocalService.deleteLayout(layout, true, new ServiceContext());

		return layout;
	}

	/**
	 * Deletes the layout, its child layouts, and its associated resources.
	 *
	 * @param  layout the layout
	 * @param  updateLayoutSet whether the layout set's page counter needs to be
	 *         updated
	 * @param  serviceContext the service context to be applied
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	@SystemEvent(
		action = SystemEventConstants.ACTION_SKIP,
		type = SystemEventConstants.TYPE_DELETE
	)
	public void deleteLayout(
			Layout layout, boolean updateLayoutSet,
			ServiceContext serviceContext)
		throws PortalException {

		// First layout validation

		if (layout.getParentLayoutId() ==
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {

			List<Layout> rootLayouts = layoutPersistence.findByG_P_P_Head(
				layout.getGroupId(), layout.isPrivateLayout(),
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, layout.isHead(), 0,
				2);

			if (rootLayouts.size() > 1) {
				Layout firstLayout = rootLayouts.get(0);

				if (firstLayout.getLayoutId() == layout.getLayoutId()) {
					Layout secondLayout = rootLayouts.get(1);

					layoutLocalServiceHelper.validateFirstLayout(secondLayout);
				}
			}
		}

		// Child layouts

		List<Layout> childLayouts = layoutPersistence.findByG_P_P_Head(
			layout.getGroupId(), layout.isPrivateLayout(), layout.getLayoutId(),
			layout.isHead());

		for (Layout childLayout : childLayouts) {
			layoutLocalService.deleteLayout(
				childLayout, updateLayoutSet, serviceContext);
		}

		// Layout friendly URLs

		layoutFriendlyURLLocalService.deleteLayoutFriendlyURLs(
			layout.getPlid());

		// Portlet preferences

		portletPreferencesLocalService.deletePortletPreferencesByPlid(
			layout.getPlid());

		// Asset

		assetEntryLocalService.deleteEntry(
			Layout.class.getName(), layout.getPlid());

		// Ratings

		ratingsStatsLocalService.deleteStats(
			Layout.class.getName(), layout.getPlid());

		// Expando

		expandoRowLocalService.deleteRows(layout.getPlid());

		// Icon

		imageLocalService.deleteImage(layout.getIconImageId());

		// Scope group

		Group scopeGroup = layout.getScopeGroup();

		if (scopeGroup != null) {
			groupLocalService.deleteGroup(scopeGroup.getGroupId());
		}

		// Resources

		String primKey =
			layout.getPlid() + PortletConstants.LAYOUT_SEPARATOR + "%";

		List<ResourcePermission> resourcePermissions =
			resourcePermissionPersistence.findByC_LikeP(
				layout.getCompanyId(), primKey);

		for (ResourcePermission resourcePermission : resourcePermissions) {
			resourcePermissionLocalService.deleteResourcePermission(
				resourcePermission);
		}

		resourceLocalService.deleteResource(
			layout.getCompanyId(), Layout.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL, layout.getPlid());

		// Draft layout

		Layout draftLayout = fetchLayout(
			classNameLocalService.getClassNameId(Layout.class),
			layout.getPlid());

		if (draftLayout != null) {
			layoutLocalService.deleteLayout(draftLayout);
		}

		// Layout

		layout = delete(layout);

		// Layout set

		if (updateLayoutSet) {
			layoutSetLocalService.updatePageCount(
				layout.getGroupId(), layout.isPrivateLayout());
		}

		// Portal preferences

		_resetPortalPreferences(layout);

		// System event

		SystemEventHierarchyEntry systemEventHierarchyEntry =
			SystemEventHierarchyEntryThreadLocal.peek();

		if ((systemEventHierarchyEntry != null) &&
			systemEventHierarchyEntry.hasTypedModel(
				Layout.class.getName(), layout.getPlid())) {

			systemEventHierarchyEntry.setExtraDataValue(
				"privateLayout", String.valueOf(layout.isPrivateLayout()));
		}

		// Indexer

		Indexer indexer = IndexerRegistryUtil.getIndexer(Layout.class);

		indexer.delete(layout);
	}

	/**
	 * Deletes the layout with the plid, also deleting the layout's child
	 * layouts, and associated resources.
	 *
	 * @param  plid the primary key of the layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout deleteLayout(long plid) throws PortalException {
		Layout layout = layoutPersistence.findByPrimaryKey(plid);

		layoutLocalService.deleteLayout(layout, true, new ServiceContext());

		return layout;
	}

	/**
	 * Deletes the layout with the layout ID, also deleting the layout's child
	 * layouts, and associated resources.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  serviceContext the service context to be applied
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void deleteLayout(
			long groupId, boolean privateLayout, long layoutId,
			ServiceContext serviceContext)
		throws PortalException {

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		layoutLocalService.deleteLayout(layout, true, serviceContext);
	}

	/**
	 * Deletes the layout with the plid, also deleting the layout's child
	 * layouts, and associated resources.
	 *
	 * @param  plid the primary key of the layout
	 * @param  serviceContext the service context to be applied
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void deleteLayout(long plid, ServiceContext serviceContext)
		throws PortalException {

		Layout layout = layoutPersistence.findByPrimaryKey(plid);

		layoutLocalService.deleteLayout(layout, true, serviceContext);
	}

	/**
	 * Deletes the group's private or non-private layouts, also deleting the
	 * layouts' child layouts, and associated resources.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  serviceContext the service context to be applied. The parent
	 *         layout set's page count will be updated by default, unless an
	 *         attribute named <code>updatePageCount</code> is set to
	 *         <code>false</code>.
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void deleteLayouts(
			long groupId, boolean privateLayout, ServiceContext serviceContext)
		throws PortalException {

		// Layouts

		List<Layout> layouts = layoutPersistence.findByG_P_P_Head(
			groupId, privateLayout, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID,
			false, QueryUtil.ALL_POS, QueryUtil.ALL_POS,
			new LayoutPriorityComparator(false));

		for (Layout layout : layouts) {
			try {
				layoutLocalService.deleteLayout(layout, false, serviceContext);
			}
			catch (NoSuchLayoutException nsle) {

				// LPS-52675

				if (_log.isDebugEnabled()) {
					_log.debug(nsle, nsle);
				}
			}
		}

		// Layout set

		if (GetterUtil.getBoolean(
				serviceContext.getAttribute("updatePageCount"), true)) {

			layoutSetLocalService.updatePageCount(groupId, privateLayout);
		}

		// Counter

		counterLocalService.reset(getCounterName(groupId, privateLayout));
	}

	@Override
	public Layout fetchDefaultLayout(long groupId, boolean privateLayout) {
		if (groupId > 0) {
			List<Layout> layouts = layoutPersistence.findByG_P_Head(
				groupId, privateLayout, false, 0, 1);

			if (!layouts.isEmpty()) {
				return layouts.get(0);
			}
		}

		return null;
	}

	@Override
	public Layout fetchDraft(Layout layout) {
		return layout;
	}

	@Override
	public Layout fetchDraft(long primaryKey) {
		return layoutPersistence.fetchByPrimaryKey(primaryKey);
	}

	@Override
	public Layout fetchFirstLayout(
		long groupId, boolean privateLayout, long parentLayoutId) {

		return layoutPersistence.fetchByG_P_P_Head_First(
			groupId, privateLayout, parentLayoutId, false,
			new LayoutPriorityComparator());
	}

	@Override
	public Layout fetchFirstLayout(
		long groupId, boolean privateLayout, long parentLayoutId,
		boolean hidden) {

		return layoutPersistence.fetchByG_P_P_H_Head_First(
			groupId, privateLayout, parentLayoutId, hidden, false,
			new LayoutPriorityComparator());
	}

	@Override
	public LayoutVersion fetchLatestVersion(Layout layout) {
		return null;
	}

	@Override
	public Layout fetchLayout(
		long groupId, boolean privateLayout, long layoutId) {

		return layoutPersistence.fetchByG_P_L_Head(
			groupId, privateLayout, layoutId, false);
	}

	@Override
	public Layout fetchLayout(long classNameId, long classPK) {
		return layoutPersistence.fetchByC_C_Head(classNameId, classPK, false);
	}

	@Override
	public Layout fetchLayout(
		String uuid, long groupId, boolean privateLayout) {

		return layoutPersistence.fetchByUUID_G_P_Head(
			uuid, groupId, privateLayout, false);
	}

	@Override
	public Layout fetchLayoutByFriendlyURL(
		long groupId, boolean privateLayout, String friendlyURL) {

		friendlyURL = layoutLocalServiceHelper.getFriendlyURL(friendlyURL);

		return layoutPersistence.fetchByG_P_F_Head(
			groupId, privateLayout, friendlyURL, false);
	}

	@Override
	public Layout fetchLayoutByIconImageId(
			boolean privateLayout, long iconImageId)
		throws PortalException {

		return layoutPersistence.fetchByP_I_Head(
			privateLayout, iconImageId, false);
	}

	/**
	 * Returns the layout matching the UUID, group, and privacy.
	 *
	 * @param  uuid the layout's UUID
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @return the matching layout, or <code>null</code> if a matching layout
	 *         could not be found
	 */
	@Override
	public Layout fetchLayoutByUuidAndGroupId(
		String uuid, long groupId, boolean privateLayout) {

		return layoutPersistence.fetchByUUID_G_P_Head(
			uuid, groupId, privateLayout, false);
	}

	@Override
	public LayoutVersion fetchLayoutVersion(long layoutVersionId) {
		return layoutVersionPersistence.fetchByPrimaryKey(layoutVersionId);
	}

	@Override
	public Layout fetchPublished(Layout layout) {
		return layout;
	}

	@Override
	public Layout fetchPublished(long primaryKey) {
		return layoutPersistence.fetchByPrimaryKey(primaryKey);
	}

	/**
	 * Returns the primary key of the default layout for the group.
	 *
	 * @param  groupId the primary key of the group
	 * @return the primary key of the default layout for the group (optionally
	 *         {@link LayoutConstants#DEFAULT_PLID})
	 */
	@Override
	public long getDefaultPlid(long groupId) {
		if (groupId > 0) {
			List<Layout> layouts = layoutPersistence.findByGroupId_Head(
				groupId, false, 0, 1);

			if (!layouts.isEmpty()) {
				Layout layout = layouts.get(0);

				return layout.getPlid();
			}
		}

		return LayoutConstants.DEFAULT_PLID;
	}

	/**
	 * Returns primary key of the matching default layout for the group
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @return the primary key of the default layout for the group; {@link
	 *         LayoutConstants#DEFAULT_PLID}) otherwise
	 */
	@Override
	public long getDefaultPlid(long groupId, boolean privateLayout) {
		Layout layout = fetchDefaultLayout(groupId, privateLayout);

		if (layout != null) {
			return layout.getPlid();
		}

		return LayoutConstants.DEFAULT_PLID;
	}

	/**
	 * Returns primary key of the default portlet layout for the group
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  portletId the primary key of the portlet
	 * @return the primary key of the default portlet layout for the group;
	 *         {@link LayoutConstants#DEFAULT_PLID} otherwise
	 * @throws PortalException
	 */
	@Override
	public long getDefaultPlid(
			long groupId, boolean privateLayout, String portletId)
		throws PortalException {

		if (groupId > 0) {
			List<Layout> layouts = layoutPersistence.findByG_P_Head(
				groupId, privateLayout, false);

			for (Layout layout : layouts) {
				if (layout.isTypePortlet()) {
					LayoutTypePortlet layoutTypePortlet =
						(LayoutTypePortlet)layout.getLayoutType();

					if (layoutTypePortlet.hasPortletId(portletId)) {
						return layout.getPlid();
					}
				}
			}
		}

		return LayoutConstants.DEFAULT_PLID;
	}

	@Override
	public Layout getDraft(Layout layout) throws PortalException {
		return layout;
	}

	@Override
	public Layout getDraft(long primaryKey) throws PortalException {
		return layoutPersistence.findByPrimaryKey(primaryKey);
	}

	/**
	 * Returns the layout for the friendly URL.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  friendlyURL the friendly URL of the layout
	 * @return the layout for the friendly URL
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout getFriendlyURLLayout(
			long groupId, boolean privateLayout, String friendlyURL)
		throws PortalException {

		if (Validator.isNull(friendlyURL)) {
			StringBundler sb = new StringBundler(5);

			sb.append("{groupId=");
			sb.append(groupId);
			sb.append(", privateLayout=");
			sb.append(privateLayout);
			sb.append("}");

			throw new NoSuchLayoutException(sb.toString());
		}

		friendlyURL = HttpUtil.decodeURL(friendlyURL);

		friendlyURL = layoutLocalServiceHelper.getFriendlyURL(friendlyURL);

		Layout layout = null;

		List<LayoutFriendlyURL> layoutFriendlyURLs =
			layoutFriendlyURLPersistence.findByG_P_F(
				groupId, privateLayout, friendlyURL, 0, 1);

		if (!layoutFriendlyURLs.isEmpty()) {
			LayoutFriendlyURL layoutFriendlyURL = layoutFriendlyURLs.get(0);

			layout = layoutPersistence.findByPrimaryKey(
				layoutFriendlyURL.getPlid());
		}

		if ((layout == null) && friendlyURL.startsWith(StringPool.SLASH) &&
			Validator.isNumber(friendlyURL.substring(1))) {

			long layoutId = GetterUtil.getLong(friendlyURL.substring(1));

			layout = layoutPersistence.fetchByG_P_L_Head(
				groupId, privateLayout, layoutId, false);
		}

		if (layout == null) {
			StringBundler sb = new StringBundler(7);

			sb.append("{groupId=");
			sb.append(groupId);
			sb.append(", privateLayout=");
			sb.append(privateLayout);
			sb.append(", friendlyURL=");
			sb.append(friendlyURL);
			sb.append("}");

			throw new NoSuchLayoutException(sb.toString());
		}

		return layout;
	}

	@Override
	public Layout getLayout(long plid) throws PortalException {
		Layout layout = layoutPersistence.findByPrimaryKey(plid);

		if (_mergeLayout(layout, plid)) {
			return layoutPersistence.findByPrimaryKey(plid);
		}

		return layout;
	}

	/**
	 * Returns the layout matching the layout ID, group, and privacy; throws a
	 * {@link NoSuchLayoutException} otherwise.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @return the matching layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout getLayout(long groupId, boolean privateLayout, long layoutId)
		throws PortalException {

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		if (_mergeLayout(layout, groupId, privateLayout, layoutId)) {
			return layoutPersistence.findByG_P_L_Head(
				groupId, privateLayout, layoutId, false);
		}

		return layout;
	}

	/**
	 * Returns the layout for the icon image; throws a {@link
	 * NoSuchLayoutException} otherwise.
	 *
	 * @param  iconImageId the primary key of the icon image
	 * @return Returns the layout for the icon image
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout getLayoutByIconImageId(long iconImageId)
		throws PortalException {

		return layoutPersistence.findByIconImageId_Head(iconImageId, false);
	}

	/**
	 * Returns the layout matching the UUID, group, and privacy.
	 *
	 * @param  uuid the layout's UUID
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @return the matching layout
	 * @throws PortalException if a matching layout could not be found
	 */
	@Override
	public Layout getLayoutByUuidAndGroupId(
			String uuid, long groupId, boolean privateLayout)
		throws PortalException {

		return layoutPersistence.findByUUID_G_P_Head(
			uuid, groupId, privateLayout, false);
	}

	/**
	 * @deprecated As of Judson (7.1.x), replaced by {@link
	 *             #getLayoutChildLayouts(List)}
	 */
	@Deprecated
	@Override
	public Map<Long, List<Layout>> getLayoutChildLayouts(
		LayoutSet layoutSet, List<Layout> parentLayouts) {

		return getLayoutChildLayouts(parentLayouts);
	}

	@Override
	public Map<Long, List<Layout>> getLayoutChildLayouts(
		List<Layout> parentLayouts) {

		Map<LayoutSet, List<Layout>> layoutsMap = new HashMap<>();

		for (Layout parentLayout : parentLayouts) {
			if (parentLayout instanceof VirtualLayout) {
				VirtualLayout virtualLayout = (VirtualLayout)parentLayout;

				Layout sourceLayout = virtualLayout.getSourceLayout();

				LayoutSet sourceLayoutSet = sourceLayout.getLayoutSet();

				List<Layout> layouts = layoutsMap.computeIfAbsent(
					sourceLayoutSet, key -> new ArrayList<>());

				layouts.add(sourceLayout);
			}
			else {
				List<Layout> layouts = layoutsMap.computeIfAbsent(
					parentLayout.getLayoutSet(), key -> new ArrayList<>());

				layouts.add(parentLayout);
			}
		}

		List<Layout> childLayouts = new ArrayList<>();

		for (Map.Entry<LayoutSet, List<Layout>> entry : layoutsMap.entrySet()) {
			List<Layout> newChildLayouts = _getChildLayouts(
				entry.getKey(),
				ListUtil.toLongArray(entry.getValue(), Layout::getLayoutId));

			childLayouts.addAll(newChildLayouts);
		}

		Map<Long, List<Layout>> layoutChildLayoutsMap = new HashMap<>();

		for (Layout childLayout : childLayouts) {
			List<Layout> layoutChildLayouts =
				layoutChildLayoutsMap.computeIfAbsent(
					childLayout.getParentPlid(),
					parentPlid -> new ArrayList<>());

			layoutChildLayouts.add(childLayout);
		}

		for (List<Layout> layoutChildLayouts : layoutChildLayoutsMap.values()) {
			layoutChildLayouts.sort(Comparator.comparing(Layout::getPriority));
		}

		return layoutChildLayoutsMap;
	}

	@Override
	public List<Layout> getLayouts(long companyId) {
		return layoutPersistence.findByCompanyId_Head(companyId, false);
	}

	/**
	 * Returns all the layouts belonging to the group.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(long groupId, boolean privateLayout) {
		return layoutPersistence.findByG_P_Head(groupId, privateLayout, false);
	}

	/**
	 * Returns a range of all the layouts belonging to the group.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  start the lower bound of the range of layouts
	 * @param  end the upper bound of the range of layouts (not inclusive)
	 * @param  obc the comparator to order the layouts
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
		long groupId, boolean privateLayout, int start, int end,
		OrderByComparator<Layout> obc) {

		return layoutPersistence.findByG_P_Head(
			groupId, privateLayout, false, start, end, obc);
	}

	/**
	 * Returns all the layouts belonging to the group that are children of the
	 * parent layout.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
		long groupId, boolean privateLayout, long parentLayoutId) {

		return getLayouts(
			groupId, privateLayout, parentLayoutId, false, QueryUtil.ALL_POS,
			QueryUtil.ALL_POS, null);
	}

	/**
	 * Returns a range of all the layouts belonging to the group that are
	 * children of the parent layout.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end -
	 * start</code> instances. <code>start</code> and <code>end</code> are not
	 * primary keys, they are indexes in the result set. Thus, <code>0</code>
	 * refers to the first result in the set. Setting both <code>start</code>
	 * and <code>end</code> to {@link QueryUtil#ALL_POS} will return the full
	 * result set.
	 * </p>
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout
	 * @param  incomplete whether the layout is incomplete
	 * @param  start the lower bound of the range of layouts
	 * @param  end the upper bound of the range of layouts (not inclusive)
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
		long groupId, boolean privateLayout, long parentLayoutId,
		boolean incomplete, int start, int end) {

		return getLayouts(
			groupId, privateLayout, parentLayoutId, incomplete, start, end,
			null);
	}

	/**
	 * Returns a range of all the layouts belonging to the group that are
	 * children of the parent layout.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end -
	 * start</code> instances. <code>start</code> and <code>end</code> are not
	 * primary keys, they are indexes in the result set. Thus, <code>0</code>
	 * refers to the first result in the set. Setting both <code>start</code>
	 * and <code>end</code> to {@link QueryUtil#ALL_POS} will return the full
	 * result set.
	 * </p>
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout
	 * @param  start the lower bound of the range of layouts
	 * @param  end the upper bound of the range of layouts (not inclusive)
	 * @param  obc the comparator to order the layouts
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
		long groupId, boolean privateLayout, long parentLayoutId,
		boolean incomplete, int start, int end, OrderByComparator<Layout> obc) {

		if (MergeLayoutPrototypesThreadLocal.isInProgress()) {
			return layoutPersistence.findByG_P_P_Head(
				groupId, privateLayout, parentLayoutId, false, start, end, obc);
		}

		try {
			Group group = groupLocalService.getGroup(groupId);

			LayoutSet layoutSet = layoutSetLocalService.getLayoutSet(
				groupId, privateLayout);

			if (layoutSet.isLayoutSetPrototypeLinkActive() &&
				!_mergeLayouts(
					group, layoutSet, groupId, privateLayout, parentLayoutId,
					start, end, obc)) {

				return layoutPersistence.findByG_P_P_Head(
					groupId, privateLayout, parentLayoutId, false, start, end,
					obc);
			}

			List<Layout> layouts = layoutPersistence.findByG_P_P_Head(
				groupId, privateLayout, parentLayoutId, false, start, end, obc);

			return _injectVirtualLayouts(
				group, layoutSet, layouts, parentLayoutId);
		}
		catch (PortalException pe) {
			throw new SystemException(pe);
		}
	}

	/**
	 * Returns all the layouts that match the layout IDs and belong to the
	 * group.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutIds the layout IDs of the layouts
	 * @return the matching layouts, or an empty list if no matches were found
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public List<Layout> getLayouts(
			long groupId, boolean privateLayout, long[] layoutIds)
		throws PortalException {

		List<Layout> layouts = new ArrayList<>();

		for (long layoutId : layoutIds) {
			Layout layout = getLayout(groupId, privateLayout, layoutId);

			layouts.add(layout);
		}

		return layouts;
	}

	/**
	 * Returns all the layouts that match the type and belong to the group.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  type the type of the layouts (optionally {@link
	 *         LayoutConstants#TYPE_PORTLET})
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
			long groupId, boolean privateLayout, String type)
		throws PortalException {

		Group group = groupPersistence.findByPrimaryKey(groupId);

		List<Layout> layouts = layoutPersistence.findByG_P_T_Head(
			groupId, privateLayout, type, false);

		if (!group.isUser()) {
			return layouts;
		}

		layouts = new ArrayList<>(layouts);

		Set<Long> checkedPlids = new HashSet<>();

		Queue<Long> checkParentLayoutIds = new ArrayDeque<>();

		checkParentLayoutIds.add(LayoutConstants.DEFAULT_PARENT_LAYOUT_ID);

		LayoutSet layoutSet = layoutSetPersistence.findByG_P_Head(
			groupId, privateLayout, false);

		while (!checkParentLayoutIds.isEmpty()) {
			long parentLayoutId = checkParentLayoutIds.poll();

			List<Layout> userGroupLayouts = _addUserGroupLayouts(
				group, layoutSet, Collections.emptyList(), parentLayoutId);

			for (Layout userGroupLayout : userGroupLayouts) {
				long userGroupPlid = userGroupLayout.getPlid();

				if (checkedPlids.add(userGroupPlid)) {
					layouts.add(userGroupLayout);

					checkParentLayoutIds.add(userGroupLayout.getLayoutId());
				}
			}
		}

		return layouts;
	}

	/**
	 * Returns a range of all the layouts belonging to the group.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  keywords keywords
	 * @param  types layout types
	 * @param  start the lower bound of the range of layouts
	 * @param  end the upper bound of the range of layouts (not inclusive)
	 * @param  obc the comparator to order the layouts
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
			long groupId, boolean privateLayout, String keywords,
			String[] types, int start, int end, OrderByComparator<Layout> obc)
		throws PortalException {

		if (Validator.isNull(keywords)) {
			return getLayouts(groupId, privateLayout, start, end, obc);
		}

		Indexer<Layout> indexer = IndexerRegistryUtil.getIndexer(
			Layout.class.getName());

		Hits hits = indexer.search(
			_buildSearchContext(
				groupId, privateLayout, keywords, types, start, end, obc));

		List<Document> documents = hits.toList();

		List<Layout> layouts = new ArrayList<>(documents.size());

		for (Document document : documents) {
			layouts.add(
				getLayout(
					GetterUtil.getLong(document.get(Field.ENTRY_CLASS_PK))));
		}

		return layouts;
	}

	/**
	 * Returns a range of all the layouts belonging to the group.
	 *
	 * @param  groupId the primary key of the group
	 * @param  start the lower bound of the range of layouts
	 * @param  end the upper bound of the range of layouts (not inclusive)
	 * @param  obc the comparator to order the layouts
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
		long groupId, int start, int end, OrderByComparator<Layout> obc) {

		return layoutPersistence.findByGroupId_Head(
			groupId, false, start, end, obc);
	}

	/**
	 * Returns the layout references for all the layouts that belong to the
	 * company and belong to the portlet that matches the preferences.
	 *
	 * @param  companyId the primary key of the company
	 * @param  portletId the primary key of the portlet
	 * @param  preferencesKey the portlet's preference key
	 * @param  preferencesValue the portlet's preference value
	 * @return the layout references of the matching layouts
	 */
	@Override
	public LayoutReference[] getLayouts(
		long companyId, String portletId, String preferencesKey,
		String preferencesValue) {

		List<LayoutReference> layoutReferences = layoutFinder.findByC_P_P(
			companyId, portletId, preferencesKey, preferencesValue);

		return layoutReferences.toArray(new LayoutReference[0]);
	}

	/**
	 * Returns a range of all the layouts belonging to the group.
	 *
	 * @param  groupId the primary key of the group
	 * @param  keywords keywords
	 * @param  types layout types
	 * @param  start the lower bound of the range of layouts
	 * @param  end the upper bound of the range of layouts (not inclusive)
	 * @param  obc the comparator to order the layouts
	 * @return the matching layouts, or <code>null</code> if no matches were
	 *         found
	 */
	@Override
	public List<Layout> getLayouts(
			long groupId, String keywords, String[] types, int start, int end,
			OrderByComparator<Layout> obc)
		throws PortalException {

		if (Validator.isNull(keywords)) {
			return getLayouts(groupId, start, end, obc);
		}

		Indexer<Layout> indexer = IndexerRegistryUtil.getIndexer(
			Layout.class.getName());

		Hits hits = indexer.search(
			_buildSearchContext(
				groupId, null, keywords, types, start, end, obc));

		List<Document> documents = hits.toList();

		List<Layout> layouts = new ArrayList<>(documents.size());

		for (Document document : documents) {
			layouts.add(
				getLayout(
					GetterUtil.getLong(document.get(Field.ENTRY_CLASS_PK))));
		}

		return layouts;
	}

	@Override
	public List<Layout> getLayoutsByLayoutPrototypeUuid(
		String layoutPrototypeUuid) {

		return layoutPersistence.findByLayoutPrototypeUuid_Head(
			layoutPrototypeUuid, false);
	}

	@Override
	public int getLayoutsByLayoutPrototypeUuidCount(
		String layoutPrototypeUuid) {

		return layoutPersistence.countByLayoutPrototypeUuid_Head(
			layoutPrototypeUuid, false);
	}

	/**
	 * Returns all the layouts matching the UUID and company.
	 *
	 * @param  uuid the UUID of the layouts
	 * @param  companyId the primary key of the company
	 * @return the matching layouts, or an empty list if no matches were found
	 */
	@Override
	public List<Layout> getLayoutsByUuidAndCompanyId(
		String uuid, long companyId) {

		return layoutPersistence.findByUuid_C_Head(uuid, companyId, false);
	}

	/**
	 * Returns a range of layouts matching the UUID and company.
	 *
	 * @param  uuid the UUID of the layouts
	 * @param  companyId the primary key of the company
	 * @param  start the lower bound of the range of layouts
	 * @param  end the upper bound of the range of layouts (not inclusive)
	 * @param  orderByComparator the comparator to order the results by
	 *         (optionally <code>null</code>)
	 * @return the range of matching layouts, or an empty list if no matches
	 *         were found
	 */
	@Override
	public List<Layout> getLayoutsByUuidAndCompanyId(
		String uuid, long companyId, int start, int end,
		OrderByComparator<Layout> orderByComparator) {

		return layoutPersistence.findByUuid_C_Head(
			uuid, companyId, false, start, end, orderByComparator);
	}

	@Override
	public int getLayoutsCount(Group group, boolean privateLayout)
		throws PortalException {

		return getLayoutsCount(group, privateLayout, true);
	}

	@Override
	public int getLayoutsCount(
			Group group, boolean privateLayout, boolean includeUserGroups)
		throws PortalException {

		int count = layoutPersistence.countByG_P_Head(
			group.getGroupId(), privateLayout, false);

		if (!group.isUser() || !includeUserGroups) {
			return count;
		}

		long[] userGroupIds = userPersistence.getUserGroupPrimaryKeys(
			group.getClassPK());

		if (userGroupIds.length != 0) {
			long userGroupClassNameId = classNameLocalService.getClassNameId(
				UserGroup.class);

			for (long userGroupId : userGroupIds) {
				Group userGroupGroup = groupPersistence.findByC_C_C(
					group.getCompanyId(), userGroupClassNameId, userGroupId);

				count += layoutPersistence.countByG_P_Head(
					userGroupGroup.getGroupId(), privateLayout, false);
			}
		}

		return count;
	}

	@Override
	public int getLayoutsCount(
		Group group, boolean privateLayout, long parentLayoutId) {

		return layoutPersistence.countByG_P_P_Head(
			group.getGroupId(), privateLayout, parentLayoutId, false);
	}

	@Override
	public int getLayoutsCount(
		Group group, boolean privateLayout, long[] layoutIds) {

		if (ArrayUtil.isEmpty(layoutIds)) {
			return 0;
		}

		DynamicQuery dynamicQuery = dynamicQuery();

		Property groupIdProperty = PropertyFactoryUtil.forName("groupId");

		dynamicQuery.add(groupIdProperty.eq(group.getGroupId()));

		Property privateLayoutProperty = PropertyFactoryUtil.forName(
			"privateLayout");

		dynamicQuery.add(privateLayoutProperty.eq(privateLayout));

		Property layoutIdProperty = PropertyFactoryUtil.forName("layoutId");

		dynamicQuery.add(layoutIdProperty.in(layoutIds));

		return GetterUtil.getInteger(dynamicQueryCount(dynamicQuery));
	}

	@Override
	public int getLayoutsCount(
			Group group, boolean privateLayout, String keywords, String[] types)
		throws PortalException {

		if (Validator.isNull(keywords)) {
			return getLayoutsCount(group, privateLayout);
		}

		Indexer<Layout> indexer = IndexerRegistryUtil.getIndexer(
			Layout.class.getName());

		Hits hits = indexer.search(
			_buildSearchContext(
				group.getGroupId(), privateLayout, keywords, types,
				QueryUtil.ALL_POS, QueryUtil.ALL_POS, null));

		return hits.getLength();
	}

	@Override
	public int getLayoutsCount(long groupId) {
		return layoutPersistence.countByGroupId_Head(groupId, false);
	}

	@Override
	public int getLayoutsCount(long groupId, String keywords, String[] types)
		throws PortalException {

		if (Validator.isNull(keywords)) {
			return getLayoutsCount(groupId);
		}

		Indexer<Layout> indexer = IndexerRegistryUtil.getIndexer(
			Layout.class.getName());

		Hits hits = indexer.search(
			_buildSearchContext(
				groupId, null, keywords, types, QueryUtil.ALL_POS,
				QueryUtil.ALL_POS, null));

		return hits.getLength();
	}

	@Override
	public int getLayoutsCount(User user, boolean privateLayout)
		throws PortalException {

		return getLayoutsCount(user, privateLayout, true);
	}

	@Override
	public int getLayoutsCount(
			User user, boolean privateLayout, boolean includeUserGroups)
		throws PortalException {

		long classNameId = classNameLocalService.getClassNameId(User.class);

		Group group = groupPersistence.findByC_C_C(
			user.getCompanyId(), classNameId, user.getUserId());

		return getLayoutsCount(group, privateLayout, includeUserGroups);
	}

	/**
	 * Returns the layout ID to use for the next layout.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @return the layout ID to use for the next layout
	 */
	@Override
	public long getNextLayoutId(long groupId, boolean privateLayout) {
		long nextLayoutId = counterLocalService.increment(
			getCounterName(groupId, privateLayout));

		if (nextLayoutId == 1) {
			List<Layout> layouts = layoutPersistence.findByG_P_Head(
				groupId, privateLayout, false, 0, 1, new LayoutComparator());

			if (!layouts.isEmpty()) {
				Layout layout = layouts.get(0);

				nextLayoutId = layout.getLayoutId() + 1;

				counterLocalService.reset(
					getCounterName(groupId, privateLayout), nextLayoutId);
			}
		}

		return nextLayoutId;
	}

	/**
	 * Returns all the layouts without resource permissions
	 *
	 * @param      roleId the primary key of the role
	 * @return     all the layouts without resource permissions
	 * @deprecated As of Judson (7.1.x), with no direct replacement
	 */
	@Deprecated
	@Override
	public List<Layout> getNoPermissionLayouts(long roleId) {
		return layoutFinder.findByNoPermissions(roleId);
	}

	/**
	 * Returns all the layouts whose friendly URLs are <code>null</code>
	 *
	 * @return     all the layouts whose friendly URLs are <code>null</code>
	 * @deprecated As of Judson (7.1.x), with no direct replacement
	 */
	@Deprecated
	@Override
	public List<Layout> getNullFriendlyURLLayouts() {
		return layoutFinder.findByNullFriendlyURL();
	}

	@Override
	public Layout getParentLayout(Layout layout) throws PortalException {
		Layout parentLayout = null;

		if (layout instanceof VirtualLayout) {
			VirtualLayout virtualLayout = (VirtualLayout)layout;

			Layout sourceLayout = virtualLayout.getSourceLayout();

			parentLayout = getLayout(
				sourceLayout.getGroupId(), sourceLayout.isPrivateLayout(),
				sourceLayout.getParentLayoutId());

			parentLayout = new VirtualLayout(parentLayout, layout.getGroup());
		}
		else {
			parentLayout = getLayout(
				layout.getGroupId(), layout.isPrivateLayout(),
				layout.getParentLayoutId());
		}

		return parentLayout;
	}

	@Override
	public List<Layout> getScopeGroupLayouts(long parentGroupId)
		throws PortalException {

		if (PropsValues.LAYOUT_SCOPE_GROUP_FINDER_ENABLED) {
			return layoutFinder.findByScopeGroup(parentGroupId);
		}

		Group parentGroup = groupPersistence.findByPrimaryKey(parentGroupId);

		if (PropsValues.LAYOUT_SCOPE_GROUP_FINDER_THRESHOLD >= 0) {
			int count = groupLocalService.getGroupsCount(
				parentGroup.getCompanyId(), Layout.class.getName(),
				parentGroupId);

			if (count >= PropsValues.LAYOUT_SCOPE_GROUP_FINDER_THRESHOLD) {
				return layoutFinder.findByScopeGroup(parentGroupId);
			}
		}

		List<Group> groups = groupLocalService.getGroups(
			parentGroup.getCompanyId(), Layout.class.getName(), parentGroupId);

		List<Layout> layouts = new ArrayList<>(groups.size());

		for (Group group : groups) {
			layouts.add(layoutPersistence.findByPrimaryKey(group.getClassPK()));
		}

		return layouts;
	}

	/**
	 * Returns all the layouts within scope of the group.
	 *
	 * @param  parentGroupId the primary key of the group's parent group
	 * @param  privateLayout whether the layout is private to the group
	 * @return the layouts within scope of the group
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public List<Layout> getScopeGroupLayouts(
			long parentGroupId, boolean privateLayout)
		throws PortalException {

		if (PropsValues.LAYOUT_SCOPE_GROUP_FINDER_ENABLED) {
			return layoutFinder.findByScopeGroup(parentGroupId, privateLayout);
		}

		Group parentGroup = groupPersistence.findByPrimaryKey(parentGroupId);

		if (PropsValues.LAYOUT_SCOPE_GROUP_FINDER_THRESHOLD >= 0) {
			int count = groupLocalService.getGroupsCount(
				parentGroup.getCompanyId(), Layout.class.getName(),
				parentGroupId);

			if (count >= PropsValues.LAYOUT_SCOPE_GROUP_FINDER_THRESHOLD) {
				return layoutFinder.findByScopeGroup(
					parentGroupId, privateLayout);
			}
		}

		List<Group> groups = groupLocalService.getGroups(
			parentGroup.getCompanyId(), Layout.class.getName(), parentGroupId);

		List<Layout> layouts = new ArrayList<>(groups.size());

		for (Group group : groups) {
			Layout layout = layoutPersistence.findByPrimaryKey(
				group.getClassPK());

			if (layout.isPrivateLayout() == privateLayout) {
				layouts.add(layout);
			}
		}

		return layouts;
	}

	@Override
	public LayoutVersion getVersion(Layout layout, int version)
		throws PortalException {

		throw new UnsupportedOperationException();
	}

	@Override
	public List<LayoutVersion> getVersions(Layout layout) {
		return Collections.emptyList();
	}

	/**
	 * Returns <code>true</code> if there is a matching layout with the UUID,
	 * group, and privacy.
	 *
	 * @param  uuid the layout's UUID
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @return <code>true</code> if the layout is found; <code>false</code>
	 *         otherwise
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public boolean hasLayout(String uuid, long groupId, boolean privateLayout)
		throws PortalException {

		try {
			getLayoutByUuidAndGroupId(uuid, groupId, privateLayout);

			return true;
		}
		catch (NoSuchLayoutException nsle) {

			// LPS-52675

			if (_log.isDebugEnabled()) {
				_log.debug(nsle, nsle);
			}
		}

		return false;
	}

	@Override
	public boolean hasLayouts(Group group) throws PortalException {
		List<LayoutSet> groupLayoutSets =
			layoutSetPersistence.findByGroupId_Head(group.getGroupId(), false);

		for (LayoutSet layoutSet : groupLayoutSets) {
			if (layoutSet.getPageCount() > 0) {
				return true;
			}
		}

		if (!group.isUser()) {
			return false;
		}

		long[] userGroupIds = userPersistence.getUserGroupPrimaryKeys(
			group.getClassPK());

		if (userGroupIds.length != 0) {
			long userGroupClassNameId = classNameLocalService.getClassNameId(
				UserGroup.class);

			for (long userGroupId : userGroupIds) {
				Group userGroupGroup = groupPersistence.findByC_C_C(
					group.getCompanyId(), userGroupClassNameId, userGroupId);

				List<LayoutSet> userGroupGroupLayoutSets =
					layoutSetPersistence.findByGroupId_Head(
						userGroupGroup.getGroupId(), false);

				for (LayoutSet layoutSet : userGroupGroupLayoutSets) {
					if (layoutSet.getPageCount() > 0) {
						return true;
					}
				}
			}
		}

		return false;
	}

	@Override
	public boolean hasLayouts(Group group, boolean privateLayout)
		throws PortalException {

		return hasLayouts(group, privateLayout, true);
	}

	@Override
	public boolean hasLayouts(
			Group group, boolean privateLayout, boolean includeUserGroups)
		throws PortalException {

		LayoutSet layoutSet = layoutSetPersistence.findByG_P_Head(
			group.getGroupId(), privateLayout, false);

		if (layoutSet.getPageCount() > 0) {
			return true;
		}

		if (!group.isUser() || !includeUserGroups) {
			return false;
		}

		long[] userGroupIds = userPersistence.getUserGroupPrimaryKeys(
			group.getClassPK());

		if (userGroupIds.length != 0) {
			long userGroupClassNameId = classNameLocalService.getClassNameId(
				UserGroup.class);

			for (long userGroupId : userGroupIds) {
				Group userGroupGroup = groupPersistence.findByC_C_C(
					group.getCompanyId(), userGroupClassNameId, userGroupId);

				layoutSet = layoutSetPersistence.findByG_P_Head(
					userGroupGroup.getGroupId(), privateLayout, false);

				if (layoutSet.getPageCount() > 0) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns <code>true</code> if the group has any layouts;
	 * <code>false</code> otherwise.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout
	 * @return <code>true</code> if the group has any layouts;
	 *         <code>false</code> otherwise
	 */
	@Override
	public boolean hasLayouts(
		long groupId, boolean privateLayout, long parentLayoutId) {

		if (layoutPersistence.countByG_P_P_Head(
				groupId, privateLayout, parentLayoutId, false) > 0) {

			return true;
		}

		return false;
	}

	@Override
	public boolean hasLayouts(User user, boolean privateLayout)
		throws PortalException {

		return hasLayouts(user, privateLayout, true);
	}

	@Override
	public boolean hasLayouts(
			User user, boolean privateLayout, boolean includeUserGroups)
		throws PortalException {

		long classNameId = classNameLocalService.getClassNameId(User.class);

		Group group = groupPersistence.findByC_C_C(
			user.getCompanyId(), classNameId, user.getUserId());

		return hasLayouts(group, privateLayout, includeUserGroups);
	}

	@Override
	public boolean hasLayoutSetPrototypeLayout(
			long layoutSetPrototypeId, String layoutUuid)
		throws PortalException {

		LayoutSetPrototype layoutSetPrototype =
			layoutSetPrototypeLocalService.getLayoutSetPrototype(
				layoutSetPrototypeId);

		return layoutLocalServiceHelper.hasLayoutSetPrototypeLayout(
			layoutSetPrototype, layoutUuid);
	}

	@Override
	public boolean hasLayoutSetPrototypeLayout(
			String layoutSetPrototypeUuid, long companyId, String layoutUuid)
		throws PortalException {

		LayoutSetPrototype layoutSetPrototype =
			layoutSetPrototypeLocalService.
				getLayoutSetPrototypeByUuidAndCompanyId(
					layoutSetPrototypeUuid, companyId);

		return layoutLocalServiceHelper.hasLayoutSetPrototypeLayout(
			layoutSetPrototype, layoutUuid);
	}

	@Override
	public Layout publishDraft(Layout layout) throws PortalException {
		return layoutPersistence.update(layout);
	}

	@Override
	public void registerListener(
		VersionServiceListener<Layout, LayoutVersion> versionServiceListener) {

		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the layouts for the group, replacing and prioritizing all layouts of
	 * the parent layout.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  parentLayoutId the layout ID of the parent layout
	 * @param  layoutIds the layout IDs of the layouts
	 * @param  serviceContext the service context to be applied
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public void setLayouts(
			long groupId, boolean privateLayout, long parentLayoutId,
			long[] layoutIds, ServiceContext serviceContext)
		throws PortalException {

		if (layoutIds == null) {
			return;
		}

		if (parentLayoutId == LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {
			if (layoutIds.length < 1) {
				throw new RequiredLayoutException(
					RequiredLayoutException.AT_LEAST_ONE);
			}

			Layout layout = layoutPersistence.findByG_P_L_Head(
				groupId, privateLayout, layoutIds[0], false);

			LayoutType layoutType = layout.getLayoutType();

			if (!layoutType.isFirstPageable()) {
				throw new RequiredLayoutException(
					RequiredLayoutException.FIRST_LAYOUT_TYPE);
			}
		}

		Set<Long> layoutIdsSet = new LinkedHashSet<>();

		for (long layoutId : layoutIds) {
			layoutIdsSet.add(layoutId);
		}

		Set<Long> newLayoutIdsSet = new HashSet<>();

		List<Layout> layouts = layoutPersistence.findByG_P_P_Head(
			groupId, privateLayout, parentLayoutId, false);

		for (Layout layout : layouts) {
			if (!layoutIdsSet.contains(layout.getLayoutId())) {
				deleteLayout(layout, true, serviceContext);
			}
			else {
				newLayoutIdsSet.add(layout.getLayoutId());
			}
		}

		int priority = 0;

		for (long layoutId : layoutIdsSet) {
			Layout layout = layoutPersistence.findByG_P_L_Head(
				groupId, privateLayout, layoutId, false);

			layout.setPriority(priority++);

			layoutLocalService.updateLayout(layout);
		}

		layoutSetLocalService.updatePageCount(groupId, privateLayout);
	}

	@Override
	public void unregisterListener(
		VersionServiceListener<Layout, LayoutVersion> versionServiceListener) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void updateAsset(
			long userId, Layout layout, long[] assetCategoryIds,
			String[] assetTagNames)
		throws PortalException {

		assetEntryLocalService.updateEntry(
			userId, layout.getGroupId(), layout.getCreateDate(),
			layout.getModifiedDate(), Layout.class.getName(), layout.getPlid(),
			layout.getUuid(), 0, assetCategoryIds, assetTagNames, true, false,
			null, null, null, null, ContentTypes.TEXT_HTML,
			layout.getName(LocaleUtil.getDefault()), null, null, null, null, 0,
			0, null);
	}

	@Override
	public Layout updateDraft(Layout layout) throws PortalException {
		return layoutPersistence.update(layout);
	}

	/**
	 * Updates the friendly URL of the layout.
	 *
	 * @param  userId the primary key of the user
	 * @param  plid the primary key of the layout
	 * @param  friendlyURL the friendly URL to be assigned
	 * @param  languageId the primary key of the language
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateFriendlyURL(
			long userId, long plid, String friendlyURL, String languageId)
		throws PortalException {

		Date now = new Date();

		Layout layout = layoutPersistence.findByPrimaryKey(plid);

		friendlyURL = layoutLocalServiceHelper.getFriendlyURL(
			layout.getGroupId(), layout.isPrivateLayout(), layout.getLayoutId(),
			StringPool.BLANK, friendlyURL);

		layoutLocalServiceHelper.validateFriendlyURL(
			layout.getGroupId(), layout.isPrivateLayout(), layout.getLayoutId(),
			friendlyURL);

		layoutFriendlyURLLocalService.updateLayoutFriendlyURL(
			userId, layout.getCompanyId(), layout.getGroupId(),
			layout.getPlid(), layout.isPrivateLayout(), friendlyURL, languageId,
			new ServiceContext());

		layout.setModifiedDate(now);

		String defaultLanguageId = LocaleUtil.toLanguageId(
			LocaleUtil.getSiteDefault());

		if (languageId.equals(defaultLanguageId)) {
			layout.setFriendlyURL(friendlyURL);
		}

		return layoutLocalService.updateLayout(layout);
	}

	@Override
	public Layout updateIconImage(long plid, byte[] bytes)
		throws PortalException {

		Layout layout = layoutPersistence.fetchByPrimaryKey(plid);

		if (layout == null) {
			return null;
		}

		Layout draftLayout = getDraft(layout);

		PortalUtil.updateImageId(
			draftLayout, true, bytes, "iconImageId", 0, 0, 0);

		return updateDraft(draftLayout);
	}

	@Override
	public Layout updateLayout(Layout layout) throws PortalException {
		return updateDraft(getDraft(layout));
	}

	/**
	 * Updates the layout replacing its draft publish date.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  publishDate the date when draft was last published
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateLayout(
			long groupId, boolean privateLayout, long layoutId,
			Date publishDate)
		throws PortalException {

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		Layout draftLayout = getDraft(layout);

		draftLayout.setPublishDate(publishDate);

		return updateDraft(draftLayout);
	}

	/**
	 * Updates the layout replacing its entity class name ID and primary key.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  classNameId the class name ID of the entity
	 * @param  classPK the primary key of the entity
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateLayout(
			long groupId, boolean privateLayout, long layoutId,
			long classNameId, long classPK)
		throws PortalException {

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		Layout draftLayout = getDraft(layout);

		draftLayout.setClassNameId(classNameId);
		draftLayout.setClassPK(classPK);

		return updateDraft(draftLayout);
	}

	/**
	 * Updates the layout.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  parentLayoutId the layout ID of the layout's new parent layout
	 * @param  nameMap the locales and localized names to merge (optionally
	 *         <code>null</code>)
	 * @param  titleMap the locales and localized titles to merge (optionally
	 *         <code>null</code>)
	 * @param  descriptionMap the locales and localized descriptions to merge
	 *         (optionally <code>null</code>)
	 * @param  keywordsMap the locales and localized keywords to merge
	 *         (optionally <code>null</code>)
	 * @param  robotsMap the locales and localized robots to merge (optionally
	 *         <code>null</code>)
	 * @param  type the layout's new type (optionally {@link
	 *         LayoutConstants#TYPE_PORTLET})
	 * @param  hidden whether the layout is hidden
	 * @param  friendlyURLMap the layout's locales and localized friendly URLs.
	 *         To see how the URL is normalized when accessed, see {@link
	 *         com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil#normalize(
	 *         String)}.
	 * @param  hasIconImage whether the icon image will be updated
	 * @param  iconBytes the byte array of the layout's new icon image
	 * @param  serviceContext the service context to be applied. Can set the
	 *         modification date and expando bridge attributes for the layout.
	 *         For layouts that are linked to a layout prototype, attributes
	 *         named <code>layoutPrototypeUuid</code> and
	 *         <code>layoutPrototypeLinkedEnabled</code> can be specified to
	 *         provide the unique identifier of the source prototype and a
	 *         boolean to determine whether a link to it should be enabled to
	 *         activate propagation of changes made to the linked page in the
	 *         prototype.
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateLayout(
			long groupId, boolean privateLayout, long layoutId,
			long parentLayoutId, Map<Locale, String> nameMap,
			Map<Locale, String> titleMap, Map<Locale, String> descriptionMap,
			Map<Locale, String> keywordsMap, Map<Locale, String> robotsMap,
			String type, boolean hidden, Map<Locale, String> friendlyURLMap,
			boolean hasIconImage, byte[] iconBytes,
			ServiceContext serviceContext)
		throws PortalException {

		// Layout

		parentLayoutId = layoutLocalServiceHelper.getParentLayoutId(
			groupId, privateLayout, parentLayoutId);

		String name = nameMap.get(LocaleUtil.getSiteDefault());

		friendlyURLMap = layoutLocalServiceHelper.getFriendlyURLMap(
			groupId, privateLayout, layoutId, name, friendlyURLMap);

		String friendlyURL = friendlyURLMap.get(LocaleUtil.getSiteDefault());

		layoutLocalServiceHelper.validate(
			groupId, privateLayout, layoutId, parentLayoutId, name, type,
			hidden, friendlyURLMap, serviceContext);

		layoutLocalServiceHelper.validateParentLayoutId(
			groupId, privateLayout, layoutId, parentLayoutId);

		Date now = new Date();

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		Layout draftLayout = getDraft(layout);

		if (parentLayoutId != layout.getParentLayoutId()) {
			draftLayout.setParentPlid(
				_getParentPlid(groupId, privateLayout, parentLayoutId));

			int priority = layoutLocalServiceHelper.getNextPriority(
				groupId, privateLayout, parentLayoutId,
				draftLayout.getSourcePrototypeLayoutUuid(), -1);

			draftLayout.setPriority(priority);
		}

		draftLayout.setModifiedDate(serviceContext.getModifiedDate(now));
		draftLayout.setParentLayoutId(parentLayoutId);
		draftLayout.setNameMap(nameMap);
		draftLayout.setTitleMap(titleMap);
		draftLayout.setDescriptionMap(descriptionMap);
		draftLayout.setKeywordsMap(keywordsMap);
		draftLayout.setRobotsMap(robotsMap);
		draftLayout.setType(type);
		draftLayout.setHidden(hidden);
		draftLayout.setFriendlyURL(friendlyURL);

		PortalUtil.updateImageId(
			draftLayout, hasIconImage, iconBytes, "iconImageId", 0, 0, 0);

		boolean layoutUpdateable = ParamUtil.getBoolean(
			serviceContext, Sites.LAYOUT_UPDATEABLE, true);

		UnicodeProperties typeSettingsProperties =
			draftLayout.getTypeSettingsProperties();

		typeSettingsProperties.put(
			Sites.LAYOUT_UPDATEABLE, String.valueOf(layoutUpdateable));

		if (privateLayout) {
			typeSettingsProperties.put(
				"privateLayout", String.valueOf(privateLayout));
		}

		draftLayout.setTypeSettingsProperties(typeSettingsProperties);

		String layoutPrototypeUuid = ParamUtil.getString(
			serviceContext, "layoutPrototypeUuid");

		if (Validator.isNotNull(layoutPrototypeUuid)) {
			draftLayout.setLayoutPrototypeUuid(layoutPrototypeUuid);

			boolean applyLayoutPrototype = ParamUtil.getBoolean(
				serviceContext, "applyLayoutPrototype");

			boolean layoutPrototypeLinkEnabled = ParamUtil.getBoolean(
				serviceContext, "layoutPrototypeLinkEnabled");

			draftLayout.setLayoutPrototypeLinkEnabled(
				layoutPrototypeLinkEnabled);

			if (applyLayoutPrototype) {
				serviceContext.setAttribute(
					"applyLayoutPrototype", Boolean.FALSE);

				_applyLayoutPrototype(
					layoutPrototypeUuid, draftLayout,
					layoutPrototypeLinkEnabled);
			}
		}

		draftLayout.setExpandoBridgeAttributes(serviceContext);

		layout = layoutLocalService.updateLayout(draftLayout);

		// Layout friendly URLs

		layoutFriendlyURLLocalService.updateLayoutFriendlyURLs(
			serviceContext.getUserId(), layout.getCompanyId(),
			layout.getGroupId(), layout.getPlid(), layout.isPrivateLayout(),
			friendlyURLMap, serviceContext);

		// Asset

		updateAsset(
			serviceContext.getUserId(), layout,
			serviceContext.getAssetCategoryIds(),
			serviceContext.getAssetTagNames());

		return layout;
	}

	/**
	 * Updates the layout replacing its type settings.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  typeSettings the settings to load the unicode properties object.
	 *         See {@link UnicodeProperties #fastLoad(String)}.
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateLayout(
			long groupId, boolean privateLayout, long layoutId,
			String typeSettings)
		throws PortalException {

		Date now = new Date();

		UnicodeProperties typeSettingsProperties = new UnicodeProperties();

		typeSettingsProperties.fastLoad(typeSettings);

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		Layout draftLayout = getDraft(layout);

		validateTypeSettingsProperties(draftLayout, typeSettingsProperties);

		draftLayout.setModifiedDate(now);
		draftLayout.setTypeSettings(typeSettingsProperties.toString());

		if (draftLayout.isSystem() && (draftLayout.getClassPK() > 0)) {
			draftLayout.setPublishDate(now);
		}

		return updateDraft(draftLayout);
	}

	/**
	 * Updates the look and feel of the layout.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  themeId the primary key of the layout's new theme
	 * @param  colorSchemeId the primary key of the layout's new color scheme
	 * @param  css the layout's new CSS
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateLookAndFeel(
			long groupId, boolean privateLayout, long layoutId, String themeId,
			String colorSchemeId, String css)
		throws PortalException {

		Date now = new Date();

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		Layout draftLayout = getDraft(layout);

		draftLayout.setModifiedDate(now);

		draftLayout.setThemeId(themeId);
		draftLayout.setColorSchemeId(colorSchemeId);
		draftLayout.setCss(css);

		return updateDraft(draftLayout);
	}

	/**
	 * Updates the name of the layout.
	 *
	 * @param  layout the layout to be updated
	 * @param  name the layout's new name
	 * @param  languageId the primary key of the language. For more information
	 *         see {@link Locale}.
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateName(Layout layout, String name, String languageId)
		throws PortalException {

		Date now = new Date();

		layoutLocalServiceHelper.validateName(name, languageId);

		Layout draftLayout = getDraft(layout);

		draftLayout.setModifiedDate(now);
		draftLayout.setName(name, LocaleUtil.fromLanguageId(languageId));

		layout = updateDraft(draftLayout);

		Group group = layout.getGroup();

		if (group.isLayoutPrototype()) {
			LayoutPrototype layoutPrototype =
				layoutPrototypeLocalService.getLayoutPrototype(
					group.getClassPK());

			layoutPrototype.setModifiedDate(now);
			layoutPrototype.setName(
				name, LocaleUtil.fromLanguageId(languageId));

			layoutPrototypePersistence.update(layoutPrototype);
		}

		return layout;
	}

	/**
	 * Updates the name of the layout matching the group, layout ID, and
	 * privacy.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  name the layout's new name
	 * @param  languageId the primary key of the language. For more information
	 *         see {@link Locale}.
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateName(
			long groupId, boolean privateLayout, long layoutId, String name,
			String languageId)
		throws PortalException {

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		return updateName(layout, name, languageId);
	}

	/**
	 * Updates the name of the layout matching the primary key.
	 *
	 * @param  plid the primary key of the layout
	 * @param  name the name to be assigned
	 * @param  languageId the primary key of the language. For more information
	 *         see {@link Locale}.
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateName(long plid, String name, String languageId)
		throws PortalException {

		Layout layout = layoutPersistence.findByPrimaryKey(plid);

		return updateName(layout, name, languageId);
	}

	/**
	 * Updates the parent layout ID of the layout matching the group, layout ID,
	 * and privacy.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  parentLayoutId the layout ID to be assigned to the parent layout
	 * @return the matching layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateParentLayoutId(
			long groupId, boolean privateLayout, long layoutId,
			long parentLayoutId)
		throws PortalException {

		parentLayoutId = layoutLocalServiceHelper.getParentLayoutId(
			groupId, privateLayout, parentLayoutId);

		layoutLocalServiceHelper.validateParentLayoutId(
			groupId, privateLayout, layoutId, parentLayoutId);

		Date now = new Date();

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		Layout draftLayout = getDraft(layout);

		if (parentLayoutId != draftLayout.getParentLayoutId()) {
			draftLayout.setParentPlid(
				_getParentPlid(groupId, privateLayout, parentLayoutId));

			int priority = layoutLocalServiceHelper.getNextPriority(
				groupId, privateLayout, parentLayoutId,
				draftLayout.getSourcePrototypeLayoutUuid(), -1);

			draftLayout.setPriority(priority);
		}

		draftLayout.setModifiedDate(now);
		draftLayout.setParentLayoutId(parentLayoutId);

		return layoutLocalService.updateLayout(draftLayout);
	}

	/**
	 * Updates the parent layout ID of the layout matching the primary key. If a
	 * layout matching the parent primary key is found, the layout ID of that
	 * layout is assigned, otherwise {@link
	 * LayoutConstants#DEFAULT_PARENT_LAYOUT_ID} is assigned.
	 *
	 * @param  plid the primary key of the layout
	 * @param  parentPlid the primary key of the parent layout
	 * @return the layout matching the primary key
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateParentLayoutId(long plid, long parentPlid)
		throws PortalException {

		Layout layout = layoutPersistence.findByPrimaryKey(plid);

		if (layout.getParentPlid() == parentPlid) {
			return layout;
		}

		Layout draftLayout = getDraft(layout);

		Date now = new Date();

		long parentLayoutId = LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;

		if (parentPlid > 0) {
			Layout parentLayout = layoutPersistence.fetchByPrimaryKey(
				parentPlid);

			if (parentLayout != null) {
				parentLayoutId = parentLayout.getLayoutId();
			}
		}

		parentLayoutId = layoutLocalServiceHelper.getParentLayoutId(
			draftLayout.getGroupId(), draftLayout.isPrivateLayout(),
			parentLayoutId);

		layoutLocalServiceHelper.validateParentLayoutId(
			draftLayout.getGroupId(), draftLayout.isPrivateLayout(),
			draftLayout.getLayoutId(), parentLayoutId);

		if (parentLayoutId != layout.getParentLayoutId()) {
			int priority = layoutLocalServiceHelper.getNextPriority(
				draftLayout.getGroupId(), draftLayout.isPrivateLayout(),
				parentLayoutId, draftLayout.getSourcePrototypeLayoutUuid(), -1);

			draftLayout.setPriority(priority);
		}

		draftLayout.setModifiedDate(now);
		draftLayout.setParentPlid(parentPlid);
		draftLayout.setParentLayoutId(parentLayoutId);

		return layoutLocalService.updateLayout(draftLayout);
	}

	/**
	 * Updates the parent layout ID and priority of the layout.
	 *
	 * @param  plid the primary key of the layout
	 * @param  parentPlid the primary key of the parent layout
	 * @param  priority the layout's new priority
	 * @return the layout matching the primary key
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updateParentLayoutIdAndPriority(
			long plid, long parentPlid, int priority)
		throws PortalException {

		Layout layout = updateParentLayoutId(plid, parentPlid);

		return layoutLocalService.updatePriority(layout, priority);
	}

	/**
	 * Updates the priorities of the layouts.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @throws PortalException
	 */
	@Override
	public void updatePriorities(long groupId, boolean privateLayout)
		throws PortalException {

		List<Layout> layouts = layoutPersistence.findByG_P_Head(
			groupId, privateLayout, false);

		for (Layout layout : layouts) {
			int nextPriority = layoutLocalServiceHelper.getNextPriority(
				layout.getGroupId(), layout.isPrivateLayout(),
				layout.getParentLayoutId(),
				layout.getSourcePrototypeLayoutUuid(), layout.getPriority());

			Layout draftLayout = getDraft(layout);

			draftLayout.setPriority(nextPriority);

			updateDraft(draftLayout);
		}
	}

	/**
	 * Updates the priority of the layout.
	 *
	 * @param  layout the layout to be updated
	 * @param  priority the layout's new priority
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updatePriority(Layout layout, int priority)
		throws PortalException {

		if (layout.getPriority() == priority) {
			return layout;
		}

		int oldPriority = layout.getPriority();

		int nextPriority = layoutLocalServiceHelper.getNextPriority(
			layout.getGroupId(), layout.isPrivateLayout(),
			layout.getParentLayoutId(), layout.getSourcePrototypeLayoutUuid(),
			priority);

		if (oldPriority == nextPriority) {
			return layout;
		}

		Layout draftLayout = getDraft(layout);

		draftLayout.setModifiedDate(new Date());
		draftLayout.setPriority(nextPriority);

		layout = updateDraft(draftLayout);

		List<Layout> layouts = layoutPersistence.findByG_P_P_Head(
			layout.getGroupId(), layout.isPrivateLayout(),
			layout.getParentLayoutId(), false);

		boolean lessThan = false;

		if (oldPriority < nextPriority) {
			lessThan = true;
		}

		layouts = ListUtil.sort(
			layouts, new LayoutPriorityComparator(layout, lessThan));

		if (layout.getParentLayoutId() ==
				LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {

			Layout firstLayout = layouts.get(0);

			layoutLocalServiceHelper.validateFirstLayout(firstLayout);
		}

		int newPriority = LayoutConstants.FIRST_PRIORITY;

		for (Layout curLayout : layouts) {
			int curNextPriority = layoutLocalServiceHelper.getNextPriority(
				layout.getGroupId(), layout.isPrivateLayout(),
				layout.getParentLayoutId(),
				curLayout.getSourcePrototypeLayoutUuid(), newPriority++);

			if (curLayout.getPriority() == curNextPriority) {
				continue;
			}

			Layout curDraftLayout = getDraft(curLayout);

			curDraftLayout.setModifiedDate(layout.getModifiedDate());
			curDraftLayout.setPriority(curNextPriority);

			curLayout = updateDraft(curDraftLayout);

			if (curLayout.equals(layout)) {
				layout = curLayout;
			}
		}

		return layout;
	}

	/**
	 * Updates the priority of the layout matching the group, layout ID, and
	 * privacy.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  priority the layout's new priority
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updatePriority(
			long groupId, boolean privateLayout, long layoutId, int priority)
		throws PortalException {

		Layout layout = layoutPersistence.findByG_P_L_Head(
			groupId, privateLayout, layoutId, false);

		return updatePriority(layout, priority);
	}

	/**
	 * Updates the priority of the layout matching the group, layout ID, and
	 * privacy, setting the layout's priority based on the priorities of the
	 * next and previous layouts.
	 *
	 * @param  groupId the primary key of the group
	 * @param  privateLayout whether the layout is private to the group
	 * @param  layoutId the layout ID of the layout
	 * @param  nextLayoutId the layout ID of the next layout
	 * @param  previousLayoutId the layout ID of the previous layout
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updatePriority(
			long groupId, boolean privateLayout, long layoutId,
			long nextLayoutId, long previousLayoutId)
		throws PortalException {

		Layout layout = getLayout(groupId, privateLayout, layoutId);

		int priority = layout.getPriority();

		Layout nextLayout = null;

		if (nextLayoutId > 0) {
			nextLayout = getLayout(groupId, privateLayout, nextLayoutId);
		}

		Layout previousLayout = null;

		if (previousLayoutId > 0) {
			previousLayout = getLayout(
				groupId, privateLayout, previousLayoutId);
		}

		if ((nextLayout != null) && (priority > nextLayout.getPriority())) {
			priority = nextLayout.getPriority();
		}
		else if ((previousLayout != null) &&
				 (priority < previousLayout.getPriority())) {

			priority = previousLayout.getPriority();
		}

		return updatePriority(layout, priority);
	}

	/**
	 * Updates the priority of the layout matching the primary key.
	 *
	 * @param  plid the primary key of the layout
	 * @param  priority the layout's new priority
	 * @return the updated layout
	 * @throws PortalException if a portal exception occurred
	 */
	@Override
	public Layout updatePriority(long plid, int priority)
		throws PortalException {

		Layout layout = layoutPersistence.findByPrimaryKey(plid);

		return updatePriority(layout, priority);
	}

	protected void validateTypeSettingsProperties(
			Layout layout, UnicodeProperties typeSettingsProperties)
		throws PortalException {

		String sitemapChangeFrequency = typeSettingsProperties.getProperty(
			"sitemap-changefreq");

		if (Validator.isNotNull(sitemapChangeFrequency) &&
			!sitemapChangeFrequency.equals("always") &&
			!sitemapChangeFrequency.equals("hourly") &&
			!sitemapChangeFrequency.equals("daily") &&
			!sitemapChangeFrequency.equals("weekly") &&
			!sitemapChangeFrequency.equals("monthly") &&
			!sitemapChangeFrequency.equals("yearly") &&
			!sitemapChangeFrequency.equals("never")) {

			throw new SitemapChangeFrequencyException();
		}

		String sitemapInclude = typeSettingsProperties.getProperty(
			LayoutTypePortletConstants.SITEMAP_INCLUDE);

		if (Validator.isNotNull(sitemapInclude) &&
			!sitemapInclude.equals("0") && !sitemapInclude.equals("1")) {

			throw new SitemapIncludeException();
		}

		String sitemapPriority = typeSettingsProperties.getProperty(
			"sitemap-priority");

		if (Validator.isNotNull(sitemapPriority)) {
			try {
				double priority = Double.parseDouble(sitemapPriority);

				if ((priority < 0) || (priority > 1)) {
					throw new SitemapPagePriorityException();
				}
			}
			catch (NumberFormatException nfe) {
				throw new SitemapPagePriorityException(nfe);
			}
		}

		boolean enableJavaScript =
			PropsValues.
				FIELD_ENABLE_COM_LIFERAY_PORTAL_KERNEL_MODEL_LAYOUT_JAVASCRIPT;

		if (!enableJavaScript) {
			UnicodeProperties layoutTypeSettingsProperties =
				layout.getTypeSettingsProperties();

			String javaScript = layoutTypeSettingsProperties.getProperty(
				"javascript");

			typeSettingsProperties.setProperty("javascript", javaScript);
		}
	}

	@BeanReference(type = LayoutLocalServiceHelper.class)
	protected LayoutLocalServiceHelper layoutLocalServiceHelper;

	private List<Layout> _addChildUserGroupLayouts(
			Group group, List<Layout> layouts)
		throws PortalException {

		List<Layout> childLayouts = new ArrayList<>(layouts.size());

		for (Layout layout : layouts) {
			Group layoutGroup = layout.getGroup();

			if (layoutGroup.isUserGroup()) {
				childLayouts.add(new VirtualLayout(layout, group));
			}
			else {
				childLayouts.add(layout);
			}
		}

		return childLayouts;
	}

	private List<Layout> _addUserGroupLayouts(
			Group group, LayoutSet layoutSet, List<Layout> layouts,
			long parentLayoutId)
		throws PortalException {

		layouts = new ArrayList<>(layouts);

		List<UserGroup> userUserGroups =
			userGroupLocalService.getUserUserGroups(group.getClassPK());

		for (UserGroup userGroup : userUserGroups) {
			Group userGroupGroup = userGroup.getGroup();

			List<Layout> userGroupLayouts = getLayouts(
				userGroupGroup.getGroupId(), layoutSet.isPrivateLayout(),
				parentLayoutId);

			for (Layout userGroupLayout : userGroupLayouts) {
				layouts.add(new VirtualLayout(userGroupLayout, group));
			}
		}

		return layouts;
	}

	private List<Layout> _addUserGroupLayouts(
			Group group, LayoutSet layoutSet, List<Layout> layouts,
			long[] parentLayoutIds)
		throws PortalException {

		boolean copied = false;

		List<UserGroup> userUserGroups =
			userGroupLocalService.getUserUserGroups(group.getClassPK());

		for (UserGroup userGroup : userUserGroups) {
			Group userGroupGroup = userGroup.getGroup();

			List<Layout> userGroupLayouts = getLayouts(
				userGroupGroup.getGroupId(), layoutSet.isPrivateLayout(),
				parentLayoutIds);

			for (Layout userGroupLayout : userGroupLayouts) {
				if (!copied) {
					layouts = new ArrayList<>(layouts);

					copied = true;
				}

				layouts.add(new VirtualLayout(userGroupLayout, group));
			}
		}

		return layouts;
	}

	private void _applyLayoutPrototype(
			String layoutPrototypeUuid, Layout layout,
			boolean layoutPrototypeLinkEnabled)
		throws PortalException {

		LayoutPrototype layoutPrototype =
			layoutPrototypeLocalService.getLayoutPrototypeByUuidAndCompanyId(
				layoutPrototypeUuid, layout.getCompanyId());

		try {
			SitesUtil.applyLayoutPrototype(
				layoutPrototype, layout, layoutPrototypeLinkEnabled);
		}
		catch (PortalException pe) {
			throw pe;
		}
		catch (SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
	}

	private SearchContext _buildSearchContext(
			long groupId, Boolean privateLayout, String keywords,
			String[] types, int start, int end, OrderByComparator<Layout> obc)
		throws PortalException {

		SearchContext searchContext = new SearchContext();

		searchContext.setAttribute(Field.TITLE, keywords);
		searchContext.setAttribute(Field.TYPE, types);
		searchContext.setAttribute("paginationType", "more");

		if (privateLayout != null) {
			searchContext.setAttribute(
				"privateLayout", String.valueOf(privateLayout));
		}

		Group group = groupLocalService.getGroup(groupId);

		searchContext.setCompanyId(group.getCompanyId());

		searchContext.setEnd(end);
		searchContext.setGroupIds(new long[] {groupId});
		searchContext.setKeywords(keywords);
		searchContext.setStart(start);

		if (obc != null) {
			searchContext.setSorts(_getSortFromComparator(obc));
		}

		QueryConfig queryConfig = searchContext.getQueryConfig();

		queryConfig.setHighlightEnabled(false);
		queryConfig.setScoreEnabled(false);

		return searchContext;
	}

	private List<Layout> _getChildLayouts(
		LayoutSet layoutSet, long[] parentLayoutIds) {

		if (MergeLayoutPrototypesThreadLocal.isInProgress()) {
			return layoutPersistence.findByG_P_P_Head(
				layoutSet.getGroupId(), layoutSet.isPrivateLayout(),
				parentLayoutIds, false);
		}

		try {
			Group group = groupPersistence.findByPrimaryKey(
				layoutSet.getGroupId());

			if (layoutSet.isLayoutSetPrototypeLinkActive() &&
				!_mergeLayouts(
					group, layoutSet, layoutSet.getGroupId(),
					layoutSet.isPrivateLayout(), parentLayoutIds)) {

				return layoutPersistence.findByG_P_P_Head(
					layoutSet.getGroupId(), layoutSet.isPrivateLayout(),
					parentLayoutIds, false);
			}

			List<Layout> layouts = layoutPersistence.findByG_P_P_Head(
				layoutSet.getGroupId(), layoutSet.isPrivateLayout(),
				parentLayoutIds, false);

			return _injectVirtualLayouts(
				group, layoutSet, layouts, parentLayoutIds);
		}
		catch (PortalException pe) {
			throw new SystemException(pe);
		}
	}

	private long _getParentPlid(
		long groupId, boolean privateLayout, long parentLayoutId) {

		if (parentLayoutId == LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {
			return 0;
		}

		Layout parentLayout = layoutPersistence.fetchByG_P_L_Head(
			groupId, privateLayout, parentLayoutId, false);

		if (parentLayout == null) {
			return 0;
		}

		return parentLayout.getPlid();
	}

	private Sort _getSortFromComparator(OrderByComparator<Layout> obc) {
		String[] fields = obc.getOrderByFields();

		boolean reverse = !obc.isAscending();
		String field = fields[0];

		return new Sort(field, Sort.LONG_TYPE, reverse);
	}

	private List<Layout> _injectVirtualLayouts(
			Group group, LayoutSet layoutSet, List<Layout> layouts,
			long parentLayoutId)
		throws PortalException {

		if (MergeLayoutPrototypesThreadLocal.isInProgress() ||
			PropsValues.USER_GROUPS_COPY_LAYOUTS_TO_USER_PERSONAL_SITE) {

			return layouts;
		}

		if (group.isUser()) {
			_virtualLayoutTargetGroupId.set(group.getGroupId());

			if (parentLayoutId == LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {
				return _addUserGroupLayouts(
					group, layoutSet, layouts, parentLayoutId);
			}

			return _addChildUserGroupLayouts(group, layouts);
		}

		if (group.isUserGroup() &&
			(parentLayoutId != LayoutConstants.DEFAULT_PARENT_LAYOUT_ID)) {

			long targetGroupId = _virtualLayoutTargetGroupId.get();

			if (targetGroupId != GroupConstants.DEFAULT_LIVE_GROUP_ID) {
				Group targetGroup = groupLocalService.getGroup(targetGroupId);

				return _addChildUserGroupLayouts(targetGroup, layouts);
			}
		}

		return layouts;
	}

	private List<Layout> _injectVirtualLayouts(
			Group group, LayoutSet layoutSet, List<Layout> layouts,
			long[] parentLayoutIds)
		throws PortalException {

		if (MergeLayoutPrototypesThreadLocal.isInProgress() ||
			PropsValues.USER_GROUPS_COPY_LAYOUTS_TO_USER_PERSONAL_SITE) {

			return layouts;
		}

		if (group.isUser()) {
			_virtualLayoutTargetGroupId.set(group.getGroupId());

			if (ArrayUtil.contains(
					parentLayoutIds,
					LayoutConstants.DEFAULT_PARENT_LAYOUT_ID)) {

				_addUserGroupLayouts(
					group, layoutSet, layouts, parentLayoutIds);

				if (parentLayoutIds.length == 1) {
					return layouts;
				}
			}

			return _addChildUserGroupLayouts(group, layouts);
		}

		if (group.isUserGroup()) {
			long targetGroupId = _virtualLayoutTargetGroupId.get();

			if (targetGroupId != GroupConstants.DEFAULT_LIVE_GROUP_ID) {
				Group targetGroup = groupLocalService.getGroup(targetGroupId);

				return _addChildUserGroupLayouts(targetGroup, layouts);
			}
		}

		return layouts;
	}

	private boolean _mergeLayout(Layout layout, Object... arguments)
		throws PortalException {

		if (MergeLayoutPrototypesThreadLocal.isInProgress()) {
			return false;
		}

		Group group = layout.getGroup();

		if (MergeLayoutPrototypesThreadLocal.isMergeComplete(
				"getLayout", arguments) &&
			(!group.isUser() ||
			 PropsValues.USER_GROUPS_COPY_LAYOUTS_TO_USER_PERSONAL_SITE)) {

			return false;
		}

		if (Validator.isNull(layout.getLayoutPrototypeUuid()) &&
			Validator.isNull(layout.getSourcePrototypeLayoutUuid())) {

			return false;
		}

		boolean workflowEnabled = WorkflowThreadLocal.isEnabled();

		LayoutSet layoutSet = layout.getLayoutSet();

		try {
			WorkflowThreadLocal.setEnabled(false);

			SitesUtil.mergeLayoutPrototypeLayout(group, layout);

			if (Validator.isNotNull(layout.getSourcePrototypeLayoutUuid())) {
				SitesUtil.mergeLayoutSetPrototypeLayouts(group, layoutSet);
			}
		}
		catch (PortalException pe) {
			throw pe;
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			MergeLayoutPrototypesThreadLocal.setMergeComplete(
				"getLayout", arguments);
			WorkflowThreadLocal.setEnabled(workflowEnabled);
		}

		return true;
	}

	private boolean _mergeLayouts(
		Group group, LayoutSet layoutSet, Object... arguments) {

		if (MergeLayoutPrototypesThreadLocal.isMergeComplete(
				"getLayouts", arguments) &&
			(!group.isUser() ||
			 PropsValues.USER_GROUPS_COPY_LAYOUTS_TO_USER_PERSONAL_SITE)) {

			return false;
		}

		boolean workflowEnabled = WorkflowThreadLocal.isEnabled();

		try {
			if (SitesUtil.isLayoutSetMergeable(group, layoutSet)) {
				WorkflowThreadLocal.setEnabled(false);

				SitesUtil.mergeLayoutSetPrototypeLayouts(group, layoutSet);
			}
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to merge layouts for site template", e);
			}
		}
		finally {
			MergeLayoutPrototypesThreadLocal.setMergeComplete(
				"getLayouts", arguments);
			WorkflowThreadLocal.setEnabled(workflowEnabled);
		}

		return true;
	}

	private void _resetPortalPreferences(Layout layout) {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
			PortalPreferences.class, getClassLoader());

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq(
				"ownerType", ResourceConstants.SCOPE_INDIVIDUAL));
		dynamicQuery.add(
			RestrictionsFactoryUtil.like(
				"preferences",
				"%" + CustomizedPages.namespacePlid(layout.getPlid()) + "%"));

		List<PortalPreferences> portalPreferenceses =
			portalPreferencesLocalService.dynamicQuery(dynamicQuery);

		for (PortalPreferences portalPreferences : portalPreferenceses) {
			PortalPreferencesImpl portalPreferencesImpl =
				new PortalPreferencesImpl(portalPreferences, false);

			portalPreferencesImpl.resetValues(
				CustomizedPages.namespacePlid(layout.getPlid()));
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		LayoutLocalServiceImpl.class);

	private static final ThreadLocal<Long> _virtualLayoutTargetGroupId =
		new CentralizedThreadLocal<>(
			LayoutLocalServiceImpl.class + "._virtualLayoutTargetGroupId",
			() -> GroupConstants.DEFAULT_LIVE_GROUP_ID);

}