package org.syncany.plugins.transfer;

import java.lang.reflect.Field;
import java.util.List;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.syncany.config.UserConfig;
import org.syncany.util.ReflectionUtil;

import com.google.common.collect.Lists;

/**
 * Converter to encrypt fields marked with the {@link Encrypted}
 * annotation. Every marked field is encrypted with the user-specific
 * config key from {@link UserConfig}.
 * 
 * @author Christian Roth <christian.roth@port17.de>
 */
public class EncryptedTransferSettingsConverter implements Converter<String> {
	private List<String> encryptedFields;

	public EncryptedTransferSettingsConverter() {
		// Nothing.
	}

	public EncryptedTransferSettingsConverter(Class<? extends TransferSettings> transferSettingsClass) {
		this.encryptedFields = getEncryptedFields(transferSettingsClass);
	}

	@Override
	public String read(InputNode node) throws Exception {
		InputNode encryptedAttribute = node.getAttribute("encrypted");
		
		if (encryptedAttribute != null && encryptedAttribute.getValue().equals(Boolean.TRUE.toString())) {
			return TransferSettings.decrypt(node.getValue());
		}

		return node.getValue();
	}

	@Override
	public void write(OutputNode node, String raw) throws Exception {
		if (encryptedFields.contains(node.getName())) {
			node.setValue(TransferSettings.encrypt(raw));
			node.setAttribute("encrypted", Boolean.TRUE.toString());
		}
		else {
			node.setValue(raw);
		}
	}

	private List<String> getEncryptedFields(Class<? extends TransferSettings> clazz) {
		List<String> encryptedFields = Lists.newArrayList();
		
		for (Field field : ReflectionUtil.getAllFieldsWithAnnotation(clazz, Encrypted.class)) {
			encryptedFields.add(field.getName());
		}
		
		return encryptedFields;
	}
}
