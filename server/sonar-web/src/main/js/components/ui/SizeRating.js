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
import { match, when } from 'match-when';
import './SizeRating.css';

export default class SizeRating extends React.Component {
  static propTypes = {
    value: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string])
  };

  render () {
    if (this.props.value == null) {
      return (
          <div className="size-rating size-rating-muted">&nbsp;</div>
      );
    }

    const letter = match({
      [when.range(0, 999)]: 'XS',
      [when.range(1000, 9999)]: 'S',
      [when.range(10000, 99999)]: 'M',
      [when.range(100000, 500000)]: 'L',
      [when()]: 'XL'
    })(Number(this.props.value));

    return (
        <div className="size-rating">{letter}</div>
    );
  }
}
