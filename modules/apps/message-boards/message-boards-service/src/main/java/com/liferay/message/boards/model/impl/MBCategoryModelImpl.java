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

package com.liferay.message.boards.model.impl;

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.util.ExpandoBridgeFactoryUtil;
import com.liferay.exportimport.kernel.lar.StagedModelType;
import com.liferay.message.boards.model.MBCategory;
import com.liferay.message.boards.model.MBCategoryModel;
import com.liferay.message.boards.model.MBCategorySoap;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.bean.AutoEscapeBeanHandler;
import com.liferay.portal.kernel.exception.NoSuchModelException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSON;
import com.liferay.portal.kernel.model.CacheModel;
import com.liferay.portal.kernel.model.ContainerModel;
import com.liferay.portal.kernel.model.ModelWrapper;
import com.liferay.portal.kernel.model.TrashedModel;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.impl.BaseModelImpl;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.Serializable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The base model implementation for the MBCategory service. Represents a row in the &quot;MBCategory&quot; database table, with each column mapped to a property of this class.
 *
 * <p>
 * This implementation and its corresponding interface <code>MBCategoryModel</code> exist only as a container for the default property accessors generated by ServiceBuilder. Helper methods and all application logic should be put in {@link MBCategoryImpl}.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see MBCategoryImpl
 * @generated
 */
@JSON(strict = true)
public class MBCategoryModelImpl
	extends BaseModelImpl<MBCategory> implements MBCategoryModel {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. All methods that expect a message boards category model instance should use the <code>MBCategory</code> interface instead.
	 */
	public static final String TABLE_NAME = "MBCategory";

	public static final Object[][] TABLE_COLUMNS = {
		{"uuid_", Types.VARCHAR}, {"categoryId", Types.BIGINT},
		{"groupId", Types.BIGINT}, {"companyId", Types.BIGINT},
		{"userId", Types.BIGINT}, {"userName", Types.VARCHAR},
		{"createDate", Types.TIMESTAMP}, {"modifiedDate", Types.TIMESTAMP},
		{"parentCategoryId", Types.BIGINT}, {"name", Types.VARCHAR},
		{"description", Types.VARCHAR}, {"displayStyle", Types.VARCHAR},
		{"messageCount", Types.INTEGER}, {"lastPostDate", Types.TIMESTAMP},
		{"lastPublishDate", Types.TIMESTAMP}, {"status", Types.INTEGER},
		{"statusByUserId", Types.BIGINT}, {"statusByUserName", Types.VARCHAR},
		{"statusDate", Types.TIMESTAMP}
	};

	public static final Map<String, Integer> TABLE_COLUMNS_MAP =
		new HashMap<String, Integer>();

	static {
		TABLE_COLUMNS_MAP.put("uuid_", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("categoryId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("groupId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("companyId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("userId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("userName", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("createDate", Types.TIMESTAMP);
		TABLE_COLUMNS_MAP.put("modifiedDate", Types.TIMESTAMP);
		TABLE_COLUMNS_MAP.put("parentCategoryId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("name", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("description", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("displayStyle", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("messageCount", Types.INTEGER);
		TABLE_COLUMNS_MAP.put("lastPostDate", Types.TIMESTAMP);
		TABLE_COLUMNS_MAP.put("lastPublishDate", Types.TIMESTAMP);
		TABLE_COLUMNS_MAP.put("status", Types.INTEGER);
		TABLE_COLUMNS_MAP.put("statusByUserId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("statusByUserName", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("statusDate", Types.TIMESTAMP);
	}

	public static final String TABLE_SQL_CREATE =
		"create table MBCategory (uuid_ VARCHAR(75) null,categoryId LONG not null primary key,groupId LONG,companyId LONG,userId LONG,userName VARCHAR(75) null,createDate DATE null,modifiedDate DATE null,parentCategoryId LONG,name VARCHAR(75) null,description STRING null,displayStyle VARCHAR(75) null,messageCount INTEGER,lastPostDate DATE null,lastPublishDate DATE null,status INTEGER,statusByUserId LONG,statusByUserName VARCHAR(75) null,statusDate DATE null)";

	public static final String TABLE_SQL_DROP = "drop table MBCategory";

	public static final String ORDER_BY_JPQL =
		" ORDER BY mbCategory.parentCategoryId ASC, mbCategory.name ASC";

	public static final String ORDER_BY_SQL =
		" ORDER BY MBCategory.parentCategoryId ASC, MBCategory.name ASC";

	public static final String DATA_SOURCE = "liferayDataSource";

	public static final String SESSION_FACTORY = "liferaySessionFactory";

	public static final String TX_MANAGER = "liferayTransactionManager";

	public static final long CATEGORYID_COLUMN_BITMASK = 1L;

	public static final long COMPANYID_COLUMN_BITMASK = 2L;

	public static final long GROUPID_COLUMN_BITMASK = 4L;

	public static final long PARENTCATEGORYID_COLUMN_BITMASK = 8L;

	public static final long STATUS_COLUMN_BITMASK = 16L;

	public static final long UUID_COLUMN_BITMASK = 32L;

	public static final long NAME_COLUMN_BITMASK = 64L;

	public static void setEntityCacheEnabled(boolean entityCacheEnabled) {
		_entityCacheEnabled = entityCacheEnabled;
	}

	public static void setFinderCacheEnabled(boolean finderCacheEnabled) {
		_finderCacheEnabled = finderCacheEnabled;
	}

	/**
	 * Converts the soap model instance into a normal model instance.
	 *
	 * @param soapModel the soap model instance to convert
	 * @return the normal model instance
	 */
	public static MBCategory toModel(MBCategorySoap soapModel) {
		if (soapModel == null) {
			return null;
		}

		MBCategory model = new MBCategoryImpl();

		model.setUuid(soapModel.getUuid());
		model.setCategoryId(soapModel.getCategoryId());
		model.setGroupId(soapModel.getGroupId());
		model.setCompanyId(soapModel.getCompanyId());
		model.setUserId(soapModel.getUserId());
		model.setUserName(soapModel.getUserName());
		model.setCreateDate(soapModel.getCreateDate());
		model.setModifiedDate(soapModel.getModifiedDate());
		model.setParentCategoryId(soapModel.getParentCategoryId());
		model.setName(soapModel.getName());
		model.setDescription(soapModel.getDescription());
		model.setDisplayStyle(soapModel.getDisplayStyle());
		model.setMessageCount(soapModel.getMessageCount());
		model.setLastPostDate(soapModel.getLastPostDate());
		model.setLastPublishDate(soapModel.getLastPublishDate());
		model.setStatus(soapModel.getStatus());
		model.setStatusByUserId(soapModel.getStatusByUserId());
		model.setStatusByUserName(soapModel.getStatusByUserName());
		model.setStatusDate(soapModel.getStatusDate());

		return model;
	}

	/**
	 * Converts the soap model instances into normal model instances.
	 *
	 * @param soapModels the soap model instances to convert
	 * @return the normal model instances
	 */
	public static List<MBCategory> toModels(MBCategorySoap[] soapModels) {
		if (soapModels == null) {
			return null;
		}

		List<MBCategory> models = new ArrayList<MBCategory>(soapModels.length);

		for (MBCategorySoap soapModel : soapModels) {
			models.add(toModel(soapModel));
		}

		return models;
	}

	public MBCategoryModelImpl() {
	}

	@Override
	public long getPrimaryKey() {
		return _categoryId;
	}

	@Override
	public void setPrimaryKey(long primaryKey) {
		setCategoryId(primaryKey);
	}

	@Override
	public Serializable getPrimaryKeyObj() {
		return _categoryId;
	}

	@Override
	public void setPrimaryKeyObj(Serializable primaryKeyObj) {
		setPrimaryKey(((Long)primaryKeyObj).longValue());
	}

	@Override
	public Class<?> getModelClass() {
		return MBCategory.class;
	}

	@Override
	public String getModelClassName() {
		return MBCategory.class.getName();
	}

	@Override
	public Map<String, Object> getModelAttributes() {
		Map<String, Object> attributes = new HashMap<String, Object>();

		Map<String, Function<MBCategory, Object>> attributeGetterFunctions =
			getAttributeGetterFunctions();

		for (Map.Entry<String, Function<MBCategory, Object>> entry :
				attributeGetterFunctions.entrySet()) {

			String attributeName = entry.getKey();
			Function<MBCategory, Object> attributeGetterFunction =
				entry.getValue();

			attributes.put(
				attributeName, attributeGetterFunction.apply((MBCategory)this));
		}

		attributes.put("entityCacheEnabled", isEntityCacheEnabled());
		attributes.put("finderCacheEnabled", isFinderCacheEnabled());

		return attributes;
	}

	@Override
	public void setModelAttributes(Map<String, Object> attributes) {
		Map<String, BiConsumer<MBCategory, Object>> attributeSetterBiConsumers =
			getAttributeSetterBiConsumers();

		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			String attributeName = entry.getKey();

			BiConsumer<MBCategory, Object> attributeSetterBiConsumer =
				attributeSetterBiConsumers.get(attributeName);

			if (attributeSetterBiConsumer != null) {
				attributeSetterBiConsumer.accept(
					(MBCategory)this, entry.getValue());
			}
		}
	}

	public Map<String, Function<MBCategory, Object>>
		getAttributeGetterFunctions() {

		return _attributeGetterFunctions;
	}

	public Map<String, BiConsumer<MBCategory, Object>>
		getAttributeSetterBiConsumers() {

		return _attributeSetterBiConsumers;
	}

	private static Function<InvocationHandler, MBCategory>
		_getProxyProviderFunction() {

		Class<?> proxyClass = ProxyUtil.getProxyClass(
			MBCategory.class.getClassLoader(), MBCategory.class,
			ModelWrapper.class);

		try {
			Constructor<MBCategory> constructor =
				(Constructor<MBCategory>)proxyClass.getConstructor(
					InvocationHandler.class);

			return invocationHandler -> {
				try {
					return constructor.newInstance(invocationHandler);
				}
				catch (ReflectiveOperationException
							reflectiveOperationException) {

					throw new InternalError(reflectiveOperationException);
				}
			};
		}
		catch (NoSuchMethodException noSuchMethodException) {
			throw new InternalError(noSuchMethodException);
		}
	}

	private static final Map<String, Function<MBCategory, Object>>
		_attributeGetterFunctions;
	private static final Map<String, BiConsumer<MBCategory, Object>>
		_attributeSetterBiConsumers;

	static {
		Map<String, Function<MBCategory, Object>> attributeGetterFunctions =
			new LinkedHashMap<String, Function<MBCategory, Object>>();
		Map<String, BiConsumer<MBCategory, ?>> attributeSetterBiConsumers =
			new LinkedHashMap<String, BiConsumer<MBCategory, ?>>();

		attributeGetterFunctions.put("uuid", MBCategory::getUuid);
		attributeSetterBiConsumers.put(
			"uuid", (BiConsumer<MBCategory, String>)MBCategory::setUuid);
		attributeGetterFunctions.put("categoryId", MBCategory::getCategoryId);
		attributeSetterBiConsumers.put(
			"categoryId",
			(BiConsumer<MBCategory, Long>)MBCategory::setCategoryId);
		attributeGetterFunctions.put("groupId", MBCategory::getGroupId);
		attributeSetterBiConsumers.put(
			"groupId", (BiConsumer<MBCategory, Long>)MBCategory::setGroupId);
		attributeGetterFunctions.put("companyId", MBCategory::getCompanyId);
		attributeSetterBiConsumers.put(
			"companyId",
			(BiConsumer<MBCategory, Long>)MBCategory::setCompanyId);
		attributeGetterFunctions.put("userId", MBCategory::getUserId);
		attributeSetterBiConsumers.put(
			"userId", (BiConsumer<MBCategory, Long>)MBCategory::setUserId);
		attributeGetterFunctions.put("userName", MBCategory::getUserName);
		attributeSetterBiConsumers.put(
			"userName",
			(BiConsumer<MBCategory, String>)MBCategory::setUserName);
		attributeGetterFunctions.put("createDate", MBCategory::getCreateDate);
		attributeSetterBiConsumers.put(
			"createDate",
			(BiConsumer<MBCategory, Date>)MBCategory::setCreateDate);
		attributeGetterFunctions.put(
			"modifiedDate", MBCategory::getModifiedDate);
		attributeSetterBiConsumers.put(
			"modifiedDate",
			(BiConsumer<MBCategory, Date>)MBCategory::setModifiedDate);
		attributeGetterFunctions.put(
			"parentCategoryId", MBCategory::getParentCategoryId);
		attributeSetterBiConsumers.put(
			"parentCategoryId",
			(BiConsumer<MBCategory, Long>)MBCategory::setParentCategoryId);
		attributeGetterFunctions.put("name", MBCategory::getName);
		attributeSetterBiConsumers.put(
			"name", (BiConsumer<MBCategory, String>)MBCategory::setName);
		attributeGetterFunctions.put("description", MBCategory::getDescription);
		attributeSetterBiConsumers.put(
			"description",
			(BiConsumer<MBCategory, String>)MBCategory::setDescription);
		attributeGetterFunctions.put(
			"displayStyle", MBCategory::getDisplayStyle);
		attributeSetterBiConsumers.put(
			"displayStyle",
			(BiConsumer<MBCategory, String>)MBCategory::setDisplayStyle);
		attributeGetterFunctions.put(
			"messageCount", MBCategory::getMessageCount);
		attributeSetterBiConsumers.put(
			"messageCount",
			(BiConsumer<MBCategory, Integer>)MBCategory::setMessageCount);
		attributeGetterFunctions.put(
			"lastPostDate", MBCategory::getLastPostDate);
		attributeSetterBiConsumers.put(
			"lastPostDate",
			(BiConsumer<MBCategory, Date>)MBCategory::setLastPostDate);
		attributeGetterFunctions.put(
			"lastPublishDate", MBCategory::getLastPublishDate);
		attributeSetterBiConsumers.put(
			"lastPublishDate",
			(BiConsumer<MBCategory, Date>)MBCategory::setLastPublishDate);
		attributeGetterFunctions.put("status", MBCategory::getStatus);
		attributeSetterBiConsumers.put(
			"status", (BiConsumer<MBCategory, Integer>)MBCategory::setStatus);
		attributeGetterFunctions.put(
			"statusByUserId", MBCategory::getStatusByUserId);
		attributeSetterBiConsumers.put(
			"statusByUserId",
			(BiConsumer<MBCategory, Long>)MBCategory::setStatusByUserId);
		attributeGetterFunctions.put(
			"statusByUserName", MBCategory::getStatusByUserName);
		attributeSetterBiConsumers.put(
			"statusByUserName",
			(BiConsumer<MBCategory, String>)MBCategory::setStatusByUserName);
		attributeGetterFunctions.put("statusDate", MBCategory::getStatusDate);
		attributeSetterBiConsumers.put(
			"statusDate",
			(BiConsumer<MBCategory, Date>)MBCategory::setStatusDate);

		_attributeGetterFunctions = Collections.unmodifiableMap(
			attributeGetterFunctions);
		_attributeSetterBiConsumers = Collections.unmodifiableMap(
			(Map)attributeSetterBiConsumers);
	}

	@JSON
	@Override
	public String getUuid() {
		if (_uuid == null) {
			return "";
		}
		else {
			return _uuid;
		}
	}

	@Override
	public void setUuid(String uuid) {
		_columnBitmask |= UUID_COLUMN_BITMASK;

		if (_originalUuid == null) {
			_originalUuid = _uuid;
		}

		_uuid = uuid;
	}

	public String getOriginalUuid() {
		return GetterUtil.getString(_originalUuid);
	}

	@JSON
	@Override
	public long getCategoryId() {
		return _categoryId;
	}

	@Override
	public void setCategoryId(long categoryId) {
		_columnBitmask |= CATEGORYID_COLUMN_BITMASK;

		if (!_setOriginalCategoryId) {
			_setOriginalCategoryId = true;

			_originalCategoryId = _categoryId;
		}

		_categoryId = categoryId;
	}

	public long getOriginalCategoryId() {
		return _originalCategoryId;
	}

	@JSON
	@Override
	public long getGroupId() {
		return _groupId;
	}

	@Override
	public void setGroupId(long groupId) {
		_columnBitmask |= GROUPID_COLUMN_BITMASK;

		if (!_setOriginalGroupId) {
			_setOriginalGroupId = true;

			_originalGroupId = _groupId;
		}

		_groupId = groupId;
	}

	public long getOriginalGroupId() {
		return _originalGroupId;
	}

	@JSON
	@Override
	public long getCompanyId() {
		return _companyId;
	}

	@Override
	public void setCompanyId(long companyId) {
		_columnBitmask |= COMPANYID_COLUMN_BITMASK;

		if (!_setOriginalCompanyId) {
			_setOriginalCompanyId = true;

			_originalCompanyId = _companyId;
		}

		_companyId = companyId;
	}

	public long getOriginalCompanyId() {
		return _originalCompanyId;
	}

	@JSON
	@Override
	public long getUserId() {
		return _userId;
	}

	@Override
	public void setUserId(long userId) {
		_userId = userId;
	}

	@Override
	public String getUserUuid() {
		try {
			User user = UserLocalServiceUtil.getUserById(getUserId());

			return user.getUuid();
		}
		catch (PortalException portalException) {
			return "";
		}
	}

	@Override
	public void setUserUuid(String userUuid) {
	}

	@JSON
	@Override
	public String getUserName() {
		if (_userName == null) {
			return "";
		}
		else {
			return _userName;
		}
	}

	@Override
	public void setUserName(String userName) {
		_userName = userName;
	}

	@JSON
	@Override
	public Date getCreateDate() {
		return _createDate;
	}

	@Override
	public void setCreateDate(Date createDate) {
		_createDate = createDate;
	}

	@JSON
	@Override
	public Date getModifiedDate() {
		return _modifiedDate;
	}

	public boolean hasSetModifiedDate() {
		return _setModifiedDate;
	}

	@Override
	public void setModifiedDate(Date modifiedDate) {
		_setModifiedDate = true;

		_modifiedDate = modifiedDate;
	}

	@JSON
	@Override
	public long getParentCategoryId() {
		return _parentCategoryId;
	}

	@Override
	public void setParentCategoryId(long parentCategoryId) {
		_columnBitmask = -1L;

		if (!_setOriginalParentCategoryId) {
			_setOriginalParentCategoryId = true;

			_originalParentCategoryId = _parentCategoryId;
		}

		_parentCategoryId = parentCategoryId;
	}

	public long getOriginalParentCategoryId() {
		return _originalParentCategoryId;
	}

	@JSON
	@Override
	public String getName() {
		if (_name == null) {
			return "";
		}
		else {
			return _name;
		}
	}

	@Override
	public void setName(String name) {
		_columnBitmask = -1L;

		_name = name;
	}

	@JSON
	@Override
	public String getDescription() {
		if (_description == null) {
			return "";
		}
		else {
			return _description;
		}
	}

	@Override
	public void setDescription(String description) {
		_description = description;
	}

	@JSON
	@Override
	public String getDisplayStyle() {
		if (_displayStyle == null) {
			return "";
		}
		else {
			return _displayStyle;
		}
	}

	@Override
	public void setDisplayStyle(String displayStyle) {
		_displayStyle = displayStyle;
	}

	@JSON
	@Override
	public int getMessageCount() {
		return _messageCount;
	}

	@Override
	public void setMessageCount(int messageCount) {
		_messageCount = messageCount;
	}

	@JSON
	@Override
	public Date getLastPostDate() {
		return _lastPostDate;
	}

	@Override
	public void setLastPostDate(Date lastPostDate) {
		_lastPostDate = lastPostDate;
	}

	@JSON
	@Override
	public Date getLastPublishDate() {
		return _lastPublishDate;
	}

	@Override
	public void setLastPublishDate(Date lastPublishDate) {
		_lastPublishDate = lastPublishDate;
	}

	@JSON
	@Override
	public int getStatus() {
		return _status;
	}

	@Override
	public void setStatus(int status) {
		_columnBitmask |= STATUS_COLUMN_BITMASK;

		if (!_setOriginalStatus) {
			_setOriginalStatus = true;

			_originalStatus = _status;
		}

		_status = status;
	}

	public int getOriginalStatus() {
		return _originalStatus;
	}

	@JSON
	@Override
	public long getStatusByUserId() {
		return _statusByUserId;
	}

	@Override
	public void setStatusByUserId(long statusByUserId) {
		_statusByUserId = statusByUserId;
	}

	@Override
	public String getStatusByUserUuid() {
		try {
			User user = UserLocalServiceUtil.getUserById(getStatusByUserId());

			return user.getUuid();
		}
		catch (PortalException portalException) {
			return "";
		}
	}

	@Override
	public void setStatusByUserUuid(String statusByUserUuid) {
	}

	@JSON
	@Override
	public String getStatusByUserName() {
		if (_statusByUserName == null) {
			return "";
		}
		else {
			return _statusByUserName;
		}
	}

	@Override
	public void setStatusByUserName(String statusByUserName) {
		_statusByUserName = statusByUserName;
	}

	@JSON
	@Override
	public Date getStatusDate() {
		return _statusDate;
	}

	@Override
	public void setStatusDate(Date statusDate) {
		_statusDate = statusDate;
	}

	@Override
	public long getContainerModelId() {
		return getCategoryId();
	}

	@Override
	public void setContainerModelId(long containerModelId) {
		_categoryId = containerModelId;
	}

	@Override
	public long getParentContainerModelId() {
		return getParentCategoryId();
	}

	@Override
	public void setParentContainerModelId(long parentContainerModelId) {
		_parentCategoryId = parentContainerModelId;
	}

	@Override
	public String getContainerModelName() {
		return String.valueOf(getName());
	}

	@Override
	public StagedModelType getStagedModelType() {
		return new StagedModelType(
			PortalUtil.getClassNameId(MBCategory.class.getName()));
	}

	@Override
	public com.liferay.trash.kernel.model.TrashEntry getTrashEntry()
		throws PortalException {

		if (!isInTrash()) {
			return null;
		}

		com.liferay.trash.kernel.model.TrashEntry trashEntry =
			com.liferay.trash.kernel.service.TrashEntryLocalServiceUtil.
				fetchEntry(getModelClassName(), getTrashEntryClassPK());

		if (trashEntry != null) {
			return trashEntry;
		}

		com.liferay.portal.kernel.trash.TrashHandler trashHandler =
			getTrashHandler();

		if (Validator.isNotNull(
				trashHandler.getContainerModelClassName(getPrimaryKey()))) {

			ContainerModel containerModel = null;

			try {
				containerModel = trashHandler.getParentContainerModel(this);
			}
			catch (NoSuchModelException noSuchModelException) {
				return null;
			}

			while (containerModel != null) {
				if (containerModel instanceof TrashedModel) {
					TrashedModel trashedModel = (TrashedModel)containerModel;

					return trashedModel.getTrashEntry();
				}

				trashHandler =
					com.liferay.portal.kernel.trash.TrashHandlerRegistryUtil.
						getTrashHandler(
							trashHandler.getContainerModelClassName(
								containerModel.getContainerModelId()));

				if (trashHandler == null) {
					return null;
				}

				containerModel = trashHandler.getContainerModel(
					containerModel.getParentContainerModelId());
			}
		}

		return null;
	}

	@Override
	public long getTrashEntryClassPK() {
		return getPrimaryKey();
	}

	/**
	 * @deprecated As of Judson (7.1.x), with no direct replacement
	 */
	@Deprecated
	@Override
	public com.liferay.portal.kernel.trash.TrashHandler getTrashHandler() {
		return com.liferay.portal.kernel.trash.TrashHandlerRegistryUtil.
			getTrashHandler(getModelClassName());
	}

	@Override
	public boolean isInTrash() {
		if (getStatus() == WorkflowConstants.STATUS_IN_TRASH) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isInTrashContainer() {
		com.liferay.portal.kernel.trash.TrashHandler trashHandler =
			getTrashHandler();

		if ((trashHandler == null) ||
			Validator.isNull(
				trashHandler.getContainerModelClassName(getPrimaryKey()))) {

			return false;
		}

		try {
			ContainerModel containerModel =
				trashHandler.getParentContainerModel(this);

			if (containerModel == null) {
				return false;
			}

			if (containerModel instanceof TrashedModel) {
				return ((TrashedModel)containerModel).isInTrash();
			}
		}
		catch (Exception exception) {
		}

		return false;
	}

	@Override
	public boolean isInTrashExplicitly() {
		if (!isInTrash()) {
			return false;
		}

		com.liferay.trash.kernel.model.TrashEntry trashEntry =
			com.liferay.trash.kernel.service.TrashEntryLocalServiceUtil.
				fetchEntry(getModelClassName(), getTrashEntryClassPK());

		if (trashEntry != null) {
			return true;
		}

		return false;
	}

	@Override
	public boolean isInTrashImplicitly() {
		if (!isInTrash()) {
			return false;
		}

		com.liferay.trash.kernel.model.TrashEntry trashEntry =
			com.liferay.trash.kernel.service.TrashEntryLocalServiceUtil.
				fetchEntry(getModelClassName(), getTrashEntryClassPK());

		if (trashEntry != null) {
			return false;
		}

		return true;
	}

	@Override
	public boolean isApproved() {
		if (getStatus() == WorkflowConstants.STATUS_APPROVED) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isDenied() {
		if (getStatus() == WorkflowConstants.STATUS_DENIED) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isDraft() {
		if (getStatus() == WorkflowConstants.STATUS_DRAFT) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isExpired() {
		if (getStatus() == WorkflowConstants.STATUS_EXPIRED) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isInactive() {
		if (getStatus() == WorkflowConstants.STATUS_INACTIVE) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isIncomplete() {
		if (getStatus() == WorkflowConstants.STATUS_INCOMPLETE) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isPending() {
		if (getStatus() == WorkflowConstants.STATUS_PENDING) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isScheduled() {
		if (getStatus() == WorkflowConstants.STATUS_SCHEDULED) {
			return true;
		}
		else {
			return false;
		}
	}

	public long getColumnBitmask() {
		return _columnBitmask;
	}

	@Override
	public ExpandoBridge getExpandoBridge() {
		return ExpandoBridgeFactoryUtil.getExpandoBridge(
			getCompanyId(), MBCategory.class.getName(), getPrimaryKey());
	}

	@Override
	public void setExpandoBridgeAttributes(ServiceContext serviceContext) {
		ExpandoBridge expandoBridge = getExpandoBridge();

		expandoBridge.setAttributes(serviceContext);
	}

	@Override
	public MBCategory toEscapedModel() {
		if (_escapedModel == null) {
			Function<InvocationHandler, MBCategory>
				escapedModelProxyProviderFunction =
					EscapedModelProxyProviderFunctionHolder.
						_escapedModelProxyProviderFunction;

			_escapedModel = escapedModelProxyProviderFunction.apply(
				new AutoEscapeBeanHandler(this));
		}

		return _escapedModel;
	}

	@Override
	public Object clone() {
		MBCategoryImpl mbCategoryImpl = new MBCategoryImpl();

		mbCategoryImpl.setUuid(getUuid());
		mbCategoryImpl.setCategoryId(getCategoryId());
		mbCategoryImpl.setGroupId(getGroupId());
		mbCategoryImpl.setCompanyId(getCompanyId());
		mbCategoryImpl.setUserId(getUserId());
		mbCategoryImpl.setUserName(getUserName());
		mbCategoryImpl.setCreateDate(getCreateDate());
		mbCategoryImpl.setModifiedDate(getModifiedDate());
		mbCategoryImpl.setParentCategoryId(getParentCategoryId());
		mbCategoryImpl.setName(getName());
		mbCategoryImpl.setDescription(getDescription());
		mbCategoryImpl.setDisplayStyle(getDisplayStyle());
		mbCategoryImpl.setMessageCount(getMessageCount());
		mbCategoryImpl.setLastPostDate(getLastPostDate());
		mbCategoryImpl.setLastPublishDate(getLastPublishDate());
		mbCategoryImpl.setStatus(getStatus());
		mbCategoryImpl.setStatusByUserId(getStatusByUserId());
		mbCategoryImpl.setStatusByUserName(getStatusByUserName());
		mbCategoryImpl.setStatusDate(getStatusDate());

		mbCategoryImpl.resetOriginalValues();

		return mbCategoryImpl;
	}

	@Override
	public int compareTo(MBCategory mbCategory) {
		int value = 0;

		if (getParentCategoryId() < mbCategory.getParentCategoryId()) {
			value = -1;
		}
		else if (getParentCategoryId() > mbCategory.getParentCategoryId()) {
			value = 1;
		}
		else {
			value = 0;
		}

		if (value != 0) {
			return value;
		}

		value = getName().compareToIgnoreCase(mbCategory.getName());

		if (value != 0) {
			return value;
		}

		return 0;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof MBCategory)) {
			return false;
		}

		MBCategory mbCategory = (MBCategory)object;

		long primaryKey = mbCategory.getPrimaryKey();

		if (getPrimaryKey() == primaryKey) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int)getPrimaryKey();
	}

	@Override
	public boolean isEntityCacheEnabled() {
		return _entityCacheEnabled;
	}

	@Override
	public boolean isFinderCacheEnabled() {
		return _finderCacheEnabled;
	}

	@Override
	public void resetOriginalValues() {
		MBCategoryModelImpl mbCategoryModelImpl = this;

		mbCategoryModelImpl._originalUuid = mbCategoryModelImpl._uuid;

		mbCategoryModelImpl._originalCategoryId =
			mbCategoryModelImpl._categoryId;

		mbCategoryModelImpl._setOriginalCategoryId = false;

		mbCategoryModelImpl._originalGroupId = mbCategoryModelImpl._groupId;

		mbCategoryModelImpl._setOriginalGroupId = false;

		mbCategoryModelImpl._originalCompanyId = mbCategoryModelImpl._companyId;

		mbCategoryModelImpl._setOriginalCompanyId = false;

		mbCategoryModelImpl._setModifiedDate = false;

		mbCategoryModelImpl._originalParentCategoryId =
			mbCategoryModelImpl._parentCategoryId;

		mbCategoryModelImpl._setOriginalParentCategoryId = false;

		mbCategoryModelImpl._originalStatus = mbCategoryModelImpl._status;

		mbCategoryModelImpl._setOriginalStatus = false;

		mbCategoryModelImpl._columnBitmask = 0;
	}

	@Override
	public CacheModel<MBCategory> toCacheModel() {
		MBCategoryCacheModel mbCategoryCacheModel = new MBCategoryCacheModel();

		mbCategoryCacheModel.uuid = getUuid();

		String uuid = mbCategoryCacheModel.uuid;

		if ((uuid != null) && (uuid.length() == 0)) {
			mbCategoryCacheModel.uuid = null;
		}

		mbCategoryCacheModel.categoryId = getCategoryId();

		mbCategoryCacheModel.groupId = getGroupId();

		mbCategoryCacheModel.companyId = getCompanyId();

		mbCategoryCacheModel.userId = getUserId();

		mbCategoryCacheModel.userName = getUserName();

		String userName = mbCategoryCacheModel.userName;

		if ((userName != null) && (userName.length() == 0)) {
			mbCategoryCacheModel.userName = null;
		}

		Date createDate = getCreateDate();

		if (createDate != null) {
			mbCategoryCacheModel.createDate = createDate.getTime();
		}
		else {
			mbCategoryCacheModel.createDate = Long.MIN_VALUE;
		}

		Date modifiedDate = getModifiedDate();

		if (modifiedDate != null) {
			mbCategoryCacheModel.modifiedDate = modifiedDate.getTime();
		}
		else {
			mbCategoryCacheModel.modifiedDate = Long.MIN_VALUE;
		}

		mbCategoryCacheModel.parentCategoryId = getParentCategoryId();

		mbCategoryCacheModel.name = getName();

		String name = mbCategoryCacheModel.name;

		if ((name != null) && (name.length() == 0)) {
			mbCategoryCacheModel.name = null;
		}

		mbCategoryCacheModel.description = getDescription();

		String description = mbCategoryCacheModel.description;

		if ((description != null) && (description.length() == 0)) {
			mbCategoryCacheModel.description = null;
		}

		mbCategoryCacheModel.displayStyle = getDisplayStyle();

		String displayStyle = mbCategoryCacheModel.displayStyle;

		if ((displayStyle != null) && (displayStyle.length() == 0)) {
			mbCategoryCacheModel.displayStyle = null;
		}

		mbCategoryCacheModel.messageCount = getMessageCount();

		Date lastPostDate = getLastPostDate();

		if (lastPostDate != null) {
			mbCategoryCacheModel.lastPostDate = lastPostDate.getTime();
		}
		else {
			mbCategoryCacheModel.lastPostDate = Long.MIN_VALUE;
		}

		Date lastPublishDate = getLastPublishDate();

		if (lastPublishDate != null) {
			mbCategoryCacheModel.lastPublishDate = lastPublishDate.getTime();
		}
		else {
			mbCategoryCacheModel.lastPublishDate = Long.MIN_VALUE;
		}

		mbCategoryCacheModel.status = getStatus();

		mbCategoryCacheModel.statusByUserId = getStatusByUserId();

		mbCategoryCacheModel.statusByUserName = getStatusByUserName();

		String statusByUserName = mbCategoryCacheModel.statusByUserName;

		if ((statusByUserName != null) && (statusByUserName.length() == 0)) {
			mbCategoryCacheModel.statusByUserName = null;
		}

		Date statusDate = getStatusDate();

		if (statusDate != null) {
			mbCategoryCacheModel.statusDate = statusDate.getTime();
		}
		else {
			mbCategoryCacheModel.statusDate = Long.MIN_VALUE;
		}

		return mbCategoryCacheModel;
	}

	@Override
	public String toString() {
		Map<String, Function<MBCategory, Object>> attributeGetterFunctions =
			getAttributeGetterFunctions();

		StringBundler sb = new StringBundler(
			4 * attributeGetterFunctions.size() + 2);

		sb.append("{");

		for (Map.Entry<String, Function<MBCategory, Object>> entry :
				attributeGetterFunctions.entrySet()) {

			String attributeName = entry.getKey();
			Function<MBCategory, Object> attributeGetterFunction =
				entry.getValue();

			sb.append(attributeName);
			sb.append("=");
			sb.append(attributeGetterFunction.apply((MBCategory)this));
			sb.append(", ");
		}

		if (sb.index() > 1) {
			sb.setIndex(sb.index() - 1);
		}

		sb.append("}");

		return sb.toString();
	}

	@Override
	public String toXmlString() {
		Map<String, Function<MBCategory, Object>> attributeGetterFunctions =
			getAttributeGetterFunctions();

		StringBundler sb = new StringBundler(
			5 * attributeGetterFunctions.size() + 4);

		sb.append("<model><model-name>");
		sb.append(getModelClassName());
		sb.append("</model-name>");

		for (Map.Entry<String, Function<MBCategory, Object>> entry :
				attributeGetterFunctions.entrySet()) {

			String attributeName = entry.getKey();
			Function<MBCategory, Object> attributeGetterFunction =
				entry.getValue();

			sb.append("<column><column-name>");
			sb.append(attributeName);
			sb.append("</column-name><column-value><![CDATA[");
			sb.append(attributeGetterFunction.apply((MBCategory)this));
			sb.append("]]></column-value></column>");
		}

		sb.append("</model>");

		return sb.toString();
	}

	private static class EscapedModelProxyProviderFunctionHolder {

		private static final Function<InvocationHandler, MBCategory>
			_escapedModelProxyProviderFunction = _getProxyProviderFunction();

	}

	private static boolean _entityCacheEnabled;
	private static boolean _finderCacheEnabled;

	private String _uuid;
	private String _originalUuid;
	private long _categoryId;
	private long _originalCategoryId;
	private boolean _setOriginalCategoryId;
	private long _groupId;
	private long _originalGroupId;
	private boolean _setOriginalGroupId;
	private long _companyId;
	private long _originalCompanyId;
	private boolean _setOriginalCompanyId;
	private long _userId;
	private String _userName;
	private Date _createDate;
	private Date _modifiedDate;
	private boolean _setModifiedDate;
	private long _parentCategoryId;
	private long _originalParentCategoryId;
	private boolean _setOriginalParentCategoryId;
	private String _name;
	private String _description;
	private String _displayStyle;
	private int _messageCount;
	private Date _lastPostDate;
	private Date _lastPublishDate;
	private int _status;
	private int _originalStatus;
	private boolean _setOriginalStatus;
	private long _statusByUserId;
	private String _statusByUserName;
	private Date _statusDate;
	private long _columnBitmask;
	private MBCategory _escapedModel;

}