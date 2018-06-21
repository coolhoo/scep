/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.scep.client;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.xipki.scep.client.exception.ScepClientException;
import org.xipki.scep.message.PkiMessage;
import org.xipki.scep.transaction.FailInfo;
import org.xipki.scep.transaction.MessageType;
import org.xipki.scep.transaction.PkiStatus;
import org.xipki.scep.util.ScepUtil;

/**
 * TODO.
 * @author Lijun Liao
 */

public final class EnrolmentResponse {

  private PkiMessage pkcsRep;

  private List<X509Certificate> certificates;

  public EnrolmentResponse(PkiMessage pkcsRep) throws ScepClientException {
    ScepUtil.requireNonNull("pkcsRep", pkcsRep);
    MessageType messageType = pkcsRep.getMessageType();
    if (MessageType.CertRep != messageType) {
      throw new ScepClientException("messageType must not be other than CertRep: " + messageType);
    }
    this.pkcsRep = pkcsRep;

    if (PkiStatus.SUCCESS != pkcsRep.getPkiStatus()) {
      return;
    }

    ASN1Encodable messageData = pkcsRep.getMessageData();
    if (!(messageData instanceof ContentInfo)) {
      throw new ScepClientException("pkcsRep is not a ContentInfo");
    }

    ContentInfo ci = (ContentInfo) messageData;
    SignedData sd = SignedData.getInstance(ci.getContent());
    ASN1Set asn1Certs = sd.getCertificates();
    if (asn1Certs == null || asn1Certs.size() == 0) {
      throw new ScepClientException("no certificate is embedded in pkcsRep");
    }

    List<X509Certificate> certs;
    try {
      certs = ScepUtil.getCertsFromSignedData(sd);
    } catch (CertificateException ex) {
      throw new ScepClientException(ex.getMessage(), ex);
    }
    this.certificates = Collections.unmodifiableList(certs);
  }

  /**
   * Returns <tt>true</tt> for a pending response, <tt>false</tt> otherwise.
   *
   * @return <tt>true</tt> for a pending response, <tt>false</tt> otherwise.
   */
  public boolean isPending() {
    return pkcsRep.getPkiStatus() == PkiStatus.PENDING;
  }

  public boolean isFailure() {
    return pkcsRep.getPkiStatus() == PkiStatus.FAILURE;
  }

  public boolean isSuccess() {
    return pkcsRep.getPkiStatus() == PkiStatus.SUCCESS;
  }

  public List<X509Certificate> getCertificates() {
    if (isSuccess()) {
      return certificates;
    }
    throw new IllegalStateException();
  }

  public FailInfo getFailInfo() {
    if (isFailure()) {
      return pkcsRep.getFailInfo();
    }
    throw new IllegalStateException();
  }

  public PkiMessage getPkcsRep() {
    return pkcsRep;
  }

}
