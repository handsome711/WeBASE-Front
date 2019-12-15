/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.front.situation;

import com.webank.webase.front.situation.entity.Situation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface SituationRepository extends CrudRepository<Situation, Long> {

    @Query(value="select s from Situation s where s.groupId = ?1 and s.timestamp between ?2 and ?3 order by s.timestamp")
    public List<Situation> findByTimeBetween(int groupId, Long startTime, Long endTime);

    @Query(value="select s from Situation s where s.groupId = ?1 and s.timestamp = max(s.timestamp)")
    public Situation findSituationDataNow(int groupId);

    @Modifying
    @Transactional
    @Query(value="delete from Situation s where s.timestamp< ?1",nativeQuery = true)
    public int deleteTimeAgo(Long time);
}
