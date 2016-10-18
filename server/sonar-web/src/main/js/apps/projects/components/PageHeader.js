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

export default class PageHeader extends React.Component {
  static propTypes = {
    total: React.PropTypes.number,
    loading: React.PropTypes.bool
  };

  render () {
    const { total, loading } = this.props;

    return (
        <header className="page-head">
          <div className="page page-limited">
            <div className="projects-list-wrapper">
              <h1 className="page-title">Projects</h1>

              {!!loading && (
                  <i className="spinner"/>
              )}

              <div className="page-actions">
                {total != null && (
                    <span><strong>{total}</strong> projects</span>
                )}
              </div>
            </div>
          </div>
        </header>
    );
  }
}
