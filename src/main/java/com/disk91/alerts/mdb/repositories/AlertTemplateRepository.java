/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.alerts.mdb.repositories;

import com.disk91.alerts.mdb.entities.AlertTemplate;
import com.disk91.alerts.mdb.entities.sub.AlertBehavior;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertTemplateRepository extends MongoRepository<AlertTemplate, String> {

    /**
     * Find an alert template by its technical id.
     * @param id - MongoDB document id
     * @return the AlertTemplate or null
     */
    AlertTemplate findOneAlertTemplateById(String id);

    /**
     * Find all alert templates matching a given behavior.
     * @param behavior - the AlertBehavior to filter on
     * @return list of matching AlertTemplate
     */
    List<AlertTemplate> findAlertTemplatesByBehavior(AlertBehavior behavior);

    /**
     * Find alert templates whose name contains the given string (case-insensitive LIKE search).
     * @param name - partial name to search for
     * @return list of matching AlertTemplate
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<AlertTemplate> findAlertTemplatesByNameLike(String name);

}
