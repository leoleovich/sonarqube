/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import React from 'react';
import ProjectCardLanguages from './ProjectCardLanguages';
import Measure from '../../component-measures/components/Measure';
import Rating from '../../../components/ui/Rating';
import CoverageRating from '../../../components/ui/CoverageRating';
import DuplicationsRating from '../../../components/ui/DuplicationsRating';
import SizeRating from '../../../components/ui/SizeRating';
import { translate } from '../../../helpers/l10n';

export default class ProjectCardMeasures extends React.Component {
  static propTypes = {
    measures: React.PropTypes.object,
    languages: React.PropTypes.array
  };

  render () {
    const { measures } = this.props;

    if (measures == null) {
      return null;
    }

    return (
        <div className="project-card-measures">
          <div className="project-card-measure">
            <div className="project-card-measure-rating">
              <Rating value={measures['reliability_rating']}/>
            </div>
            <div className="project-card-measure-inner">
              <div className="project-card-measure-number">
                <Measure measure={{ value: measures['bugs'] }}
                         metric={{ key: 'bugs', type: 'SHORT_INT' }}/>
              </div>
              <div className="project-card-measure-label">
                {translate('metric.bugs.name')}
              </div>
            </div>
          </div>

          <div className="project-card-measure">
            <div className="project-card-measure-rating">
              <Rating value={measures['security_rating']}/>
            </div>
            <div className="project-card-measure-inner">
              <div className="project-card-measure-number">
                <Measure measure={{ value: measures['vulnerabilities'] }}
                         metric={{ key: 'vulnerabilities', type: 'SHORT_INT' }}/>
              </div>
              <div className="project-card-measure-label">
                {translate('metric.vulnerabilities.name')}
              </div>
            </div>
          </div>

          <div className="project-card-measure">
            <div className="project-card-measure-rating">
              <Rating value={measures['sqale_rating']}/>
            </div>
            <div className="project-card-measure-inner">
              <div className="project-card-measure-number">
                <Measure measure={{ value: measures['sqale_index'] }}
                         metric={{ key: 'sqale_index', type: 'SHORT_WORK_DUR' }}/>
              </div>
              <div className="project-card-measure-label">
                {translate('metric.sqale_index.name')}
              </div>
            </div>
          </div>

          <div className="project-card-measure">
            <div className="project-card-measure-rating">
              <CoverageRating value={measures['coverage']}/>
            </div>
            <div className="project-card-measure-inner">
              <div className="project-card-measure-number">
                <Measure measure={{ value: measures['coverage'] }}
                         metric={{ key: 'coverage', type: 'PERCENT' }}/>
              </div>
              <div className="project-card-measure-label">
                {translate('metric.coverage.name')}
              </div>
            </div>
          </div>

          <div className="project-card-measure">
            <div className="project-card-measure-rating">
              <DuplicationsRating value={measures['duplicated_lines_density']}/>
            </div>
            <div className="project-card-measure-inner">
              <div className="project-card-measure-number">
                <Measure measure={{ value: measures['duplicated_lines_density'] }}
                         metric={{ key: 'duplicated_lines_density', type: 'PERCENT' }}/>
              </div>
              <div className="project-card-measure-label">
                {translate('metric.duplicated_lines_density.short_name')}
              </div>
            </div>
          </div>

          <div className="project-card-measure">
            <div className="project-card-measure-rating">
              <SizeRating value={measures['ncloc']}/>
            </div>
            <div className="project-card-measure-inner">
              <div className="project-card-measure-number">
                <Measure measure={{ value: measures['ncloc'] }}
                         metric={{ key: 'ncloc', type: 'SHORT_INT' }}/>
              </div>
              <div className="project-card-measure-label">
                <ProjectCardLanguages distribution={measures['ncloc_language_distribution']}/>
              </div>
            </div>
          </div>
        </div>
    );
  }
}
