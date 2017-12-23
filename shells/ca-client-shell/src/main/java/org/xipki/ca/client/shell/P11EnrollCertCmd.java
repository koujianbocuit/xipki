/*
 *
 * Copyright (c) 2013 - 2017 Lijun Liao
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

package org.xipki.ca.client.shell;

import java.security.cert.X509Certificate;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.bouncycastle.util.encoders.Hex;
import org.xipki.ca.client.shell.completer.P11ModuleNameCompleter;
import org.xipki.common.ObjectCreationException;
import org.xipki.security.ConcurrentContentSigner;
import org.xipki.security.HashAlgoType;
import org.xipki.security.SignatureAlgoControl;
import org.xipki.security.SignerConf;
import org.xipki.security.pkcs11.P11CryptServiceFactory;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "xi", name = "cmp-enroll",
        description = "enroll certificate (PKCS#11 token)")
@Service
public class P11EnrollCertCmd extends EnrollCertAction {

    @Option(name = "--slot",
            required = true,
            description = "slot index\n"
                    + "(required)")
    private Integer slotIndex;

    @Option(name = "--key-id",
            description = "id of the private key in the PKCS#11 device\n"
                    + "either keyId or keyLabel must be specified")
    private String keyId;

    @Option(name = "--key-label",
            description = "label of the private key in the PKCS#11 device\n"
                    + "either keyId or keyLabel must be specified")
    private String keyLabel;

    @Option(name = "--module",
            description = "name of the PKCS#11 module")
    @Completion(P11ModuleNameCompleter.class)
    private String moduleName = P11CryptServiceFactory.DEFAULT_P11MODULE_NAME;

    @Override
    protected ConcurrentContentSigner getSigner(final SignatureAlgoControl signatureAlgoControl)
            throws ObjectCreationException {
        byte[] keyIdBytes = null;
        if (keyId != null) {
            keyIdBytes = Hex.decode(keyId);
        }

        SignerConf signerConf = SignerConf.getPkcs11SignerConf(moduleName, slotIndex, null,
                keyLabel, keyIdBytes, 1, HashAlgoType.getNonNullHashAlgoType(hashAlgo),
                signatureAlgoControl);
        return securityFactory.createSigner("PKCS11", signerConf, (X509Certificate[]) null);
    }

}
