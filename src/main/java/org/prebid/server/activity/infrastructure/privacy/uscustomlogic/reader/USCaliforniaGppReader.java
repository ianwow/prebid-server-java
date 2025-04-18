package org.prebid.server.activity.infrastructure.privacy.uscustomlogic.reader;

import com.iab.gpp.encoder.GppModel;
import com.iab.gpp.encoder.section.UsCa;
import org.prebid.server.activity.infrastructure.privacy.uscustomlogic.USCustomLogicGppReader;
import org.prebid.server.util.ObjectUtil;

import java.util.List;

public class USCaliforniaGppReader implements USCustomLogicGppReader {

    private final UsCa consent;

    public USCaliforniaGppReader(GppModel gppModel) {
        consent = gppModel != null ? gppModel.getUsCaSection() : null;
    }

    @Override
    public Integer getVersion() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getVersion);
    }

    @Override
    public Boolean getGpc() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getGpc);
    }

    @Override
    public Boolean getGpcSegmentType() {
        return null;
    }

    @Override
    public Boolean getGpcSegmentIncluded() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getGpcSegmentIncluded);
    }

    @Override
    public Integer getSaleOptOut() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getSaleOptOut);
    }

    @Override
    public Integer getSaleOptOutNotice() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getSaleOptOutNotice);
    }

    @Override
    public Integer getSharingNotice() {
        return null;
    }

    @Override
    public Integer getSharingOptOut() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getSharingOptOut);
    }

    @Override
    public Integer getSharingOptOutNotice() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getSharingOptOutNotice);
    }

    @Override
    public Integer getTargetedAdvertisingOptOut() {
        return null;
    }

    @Override
    public Integer getTargetedAdvertisingOptOutNotice() {
        return null;
    }

    @Override
    public Integer getSensitiveDataLimitUseNotice() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getSensitiveDataLimitUseNotice);
    }

    @Override
    public List<Integer> getSensitiveDataProcessing() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getSensitiveDataProcessing);
    }

    @Override
    public Integer getSensitiveDataProcessingOptOutNotice() {
        return null;
    }

    @Override
    public List<Integer> getKnownChildSensitiveDataConsents() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getKnownChildSensitiveDataConsents);
    }

    @Override
    public Integer getPersonalDataConsents() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getPersonalDataConsents);
    }

    @Override
    public Integer getMspaCoveredTransaction() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getMspaCoveredTransaction);
    }

    @Override
    public Integer getMspaServiceProviderMode() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getMspaServiceProviderMode);
    }

    @Override
    public Integer getMspaOptOutOptionMode() {
        return ObjectUtil.getIfNotNull(consent, UsCa::getMspaOptOutOptionMode);
    }
}
