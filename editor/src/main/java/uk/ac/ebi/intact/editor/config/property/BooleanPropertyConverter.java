/**
 * Copyright 2011 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.config.property;

import org.springframework.stereotype.Component;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Component
public class BooleanPropertyConverter implements PropertyConverter<Boolean> {

    public BooleanPropertyConverter() {
    }

    @Override
    public Boolean convertFromString(String str) {
        if (str == null) return null;

        return Boolean.valueOf(str);
    }

    @Override
    public String convertToString(Boolean obj) {
        if (obj == null) return null;

        return obj.toString();
    }

    @Override
    public Class<Boolean> getObjectType() {
        return Boolean.class;
    }
}
