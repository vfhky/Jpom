///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

// https://github.com/hua1995116/ansi-color-parse
const ansiparse: any = function (str: any) {
  //
  // I'm terrible at writing parsers.
  //
  let matchingControl = null,
    matchingData = null,
    matchingText: string = '',
    ansiState: any[] = [],
    // eslint-disable-next-line prefer-const
    result: any[] = [],
    state: any = {},
    eraseChar

  //
  // General workflow for this thing is:
  // \033\[33mText
  // |     |  |
  // |     |  matchingText
  // |     matchingData
  // matchingControl
  //
  // In further steps we hope it's all going to be fine. It usually is.
  //

  //
  // Erases a char from the output
  //
  // eslint-disable-next-line prefer-const
  eraseChar = function () {
    let index, text
    if (matchingText.length) {
      matchingText = matchingText.substr(0, matchingText.length - 1)
    } else if (result.length) {
      index = result.length - 1
      text = result[index].text
      if (text.length === 1) {
        //
        // A result bit was fully deleted, pop it out to simplify the final output
        //
        result.pop()
      } else {
        result[index].text = text.substr(0, text.length - 1)
      }
    }
  }

  for (let i = 0; i < str.length; i++) {
    if (matchingControl != null) {
      if (matchingControl == '\x1b' && str[i] == '[') {
        //
        // We've matched full control code. Lets start matching formating data.
        //

        //
        // "emit" matched text with correct state
        //
        if (matchingText) {
          state.text = matchingText
          result.push(state)
          state = {}
          matchingText = ''
        }

        matchingControl = null
        matchingData = ''
      } else {
        //
        // We failed to match anything - most likely a bad control code. We
        // go back to matching regular strings.
        //
        matchingText += matchingControl + str[i]
        matchingControl = null
      }
      continue
    } else if (matchingData != null) {
      if (str[i] == ';') {
        //
        // `;` separates many formatting codes, for example: `\033[33;43m`
        // means that both `33` and `43` should be applied.
        //
        // TODO: this can be simplified by modifying state here.
        //
        ansiState.push(matchingData)
        matchingData = ''
      } else if (str[i] == 'm') {
        //
        // `m` finished whole formatting code. We can proceed to matching
        // formatted text.
        //
        ansiState.push(matchingData)
        matchingData = null
        matchingText = ''

        //
        // Convert matched formatting data into user-friendly state object.
        //
        // TODO: DRY.
        //
        ansiState.forEach(function (ansiCode) {
          if (ansiparse.foregroundColors[ansiCode]) {
            state.foreground = ansiparse.foregroundColors[ansiCode]
          } else if (ansiparse.backgroundColors[ansiCode]) {
            state.background = ansiparse.backgroundColors[ansiCode]
          } else if (ansiCode == 39) {
            delete state.foreground
          } else if (ansiCode == 49) {
            delete state.background
          } else if (ansiparse.styles[ansiCode]) {
            state[ansiparse.styles[ansiCode]] = true
          } else if (ansiCode == 22) {
            state.bold = false
          } else if (ansiCode == 23) {
            state.italic = false
          } else if (ansiCode == 24) {
            state.underline = false
          }
        })
        ansiState = []
      } else {
        matchingData += str[i]
      }
      continue
    }

    if (str[i] == '\x1b') {
      matchingControl = str[i]
    } else if (str[i] == '\u0008') {
      eraseChar()
    } else {
      matchingText += str[i]
    }
  }

  if (matchingText) {
    state.text = matchingText + (matchingControl ? matchingControl : '')
    result.push(state)
  }
  return result
}

ansiparse.foregroundColors = {
  30: 'black',
  31: 'red',
  32: 'green',
  33: 'yellow',
  34: 'blue',
  35: 'magenta',
  36: 'cyan',
  37: 'white',
  90: 'grey'
}

ansiparse.backgroundColors = {
  40: 'black',
  41: 'red',
  42: 'green',
  43: 'yellow',
  44: 'blue',
  45: 'magenta',
  46: 'cyan',
  47: 'white'
}

ansiparse.styles = {
  1: 'bold',
  3: 'italic',
  4: 'underline'
}

export default ansiparse
