/*
 * Copyright [2016] [George Papadakis (gpapadis@yahoo.gr)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package DataReader.GroundTruthReader;

import DataModel.EntityProfile;
import DataModel.IdDuplicates;
import java.util.List;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */
public class GtSerializationReader extends AbstractGtReader {
    
    public GtSerializationReader(String filePath) {
        super(filePath);
    }

    @Override
    public Set<IdDuplicates> getDuplicatePairs(List<EntityProfile> profilesD1,
            List<EntityProfile> profilesD2) {
        if (!idDuplicates.isEmpty()) {
            return idDuplicates;
        }
        
        idDuplicates.addAll((Set<IdDuplicates>) loadSerializedObject(inputFilePath));
        return idDuplicates;
    }

    @Override
    public String getMethodInfo() {
        return "Serialization Ground-truth Reader: loads a file with Java serialized IdDuplicates objects into memory.";
    }

    @Override
    public String getMethodParameters() {
        return "No other parameter is required, apart from the absolute file path";
    }
}
