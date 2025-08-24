# VS Code Live Joyride demo

[Joyride](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride) is a [VS Code](https://code.visualstudio.com/) extension that lets you customize and automate your VS Code experience. In user space (Emacs style). Joyride's Language Model tools enable Copilot to hack VS Code with you, or even for you.

<div style="position: relative; display: inline-block;">
  <a href="https://www.youtube.com/watch?v=Nt1p6yreAUU">
    <img src="https://img.youtube.com/vi/Nt1p6yreAUU/maxresdefault.jpg" alt="VS Code Live - Vibe-hack VS Code with Joyride and Copilot" style="width: 100%; max-width: 600px;">
    <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 68px; height: 48px; background: rgba(0,0,0,0.8); border-radius: 12px; cursor: pointer;">
      <svg viewBox="0 0 68 48" style="width: 100%; height: 100%;">
        <path d="M66.52,7.74c-0.78-2.93-2.49-5.41-5.42-6.19C55.79,.13,34,0,34,0S12.21,.13,6.9,1.55 C3.97,2.33,2.27,4.81,1.48,7.74C0.06,13.05,0,24,0,24s0.06,10.95,1.48,16.26c0.78,2.93,2.49,5.41,5.42,6.19 C12.21,47.87,34,48,34,48s21.79-0.13,27.1-1.55c2.93-0.78,4.64-3.26,5.42-6.19C67.94,34.95,68,24,68,24S67.94,13.05,66.52,7.74z" fill="#f00"></path>
        <path d="M 45,24 27,14 27,34" fill="#fff"></path>
      </svg>
    </div>
  </a>
</div>

[▶️ VS Code Live - Vibe-hack VS Code with Joyride and Copilot](https://www.youtube.com/watch?v=Nt1p6yreAUU)

## How to use this project

You *can* just use it for the links, but the project is actually meant to be forked, cloned, and opened in VS Code. Perfect for following along with the demo, which uses this project for a large part. **NB**: Some scripts require npm modules, so the recommended first steps are:

0. Open the project in VS Code
1. From the project root: `npm install`
1. If you don't have Joyride installed:
   1. From the Extension pane: Install Joyride
1. If you have Joyride installed 🎸:
   1. From the command palette: <kbd>Developer: Reload Window</kbd>

You should also install [Calva](https://calva.io), a Clojure extension.

When the project opens, with Joyride installed, two things happen, triggered from the Workspace activation script:

1. The slide script activates. This sets a `when` context that can be targeted from keyboard shortcuts (see below).
2. The **Audio Service** webview opens. This is for the audio playback script (see below). Browser security requires that you click the **Enable Audio** button in order for any sound to play.

### Slide show (next-slide.cljs)

The project has a script for showing and navigating slides, [next_slide.cljs](.joyride/src/next_slide.cljs), so you don't need to leave VS Code when presenting slides. There is a slide notes script that goes with it, [next_slide_notes.cljs](.joyride/src/next_slide_notes.cljs). Plus a timer-widget script. These three scripts are tailored to how I want to work with slides, but they are battle tested and work very well.

The slide notes script helps you to maintain notes to your slides. It can also generate a paginated PDF (requires [Pandoc](https://pandoc.org/installing.html)).

Both these scripts have keyboard shortcuts definitions at the top, as comments. You can uncomment, copy, and paste it in your keybindings JSON file. Adapt the actual bindings if they don't fit you. (Though pagedown, etc are for using a clicker, so don't change those if you are going to use a clicker. 😀)

### Generate audio (TTS)

There is a script that can generate speech audio from text. This is more experimental, and for the fun of it, but it works and you can build from it, if you have use cases. This one uses OpenAI's `ai-text-to-speech` npm module, and requires that you have a configured OpenAI API Key in your environment.

### Audio playback

Experimental, but mostly working fine. Uses a webview for playback, and this requires that you click the **Enable Audio** button in the view (because browser security).

### Examples namespace

TBD

## Shortcuts

Scripts/code in this project, featured in the demo:

* Examples namespace: [live_examples.cljs](.joyride/src/live_examples.cljs)
* Slide show script: [next_slide.cljs](.joyride/src/next_slide.cljs)
* Audio generation (TTS): [ai_presenter/audio_generation.cljs](.joyride/src/ai_presenter/audio_generation.cljs)
* Audio playback:
  * [ai_presenter/audio_playback.cljs](.joyride/src/ai_presenter/audio_playback.cljs)
  * [audio-service.html](.joyride/resources/audio-service.html)
* Timer status bar item: [showtime.cljs](.joyride/src/showtime.cljs)
* Workspace activation script: [workspace_activate.cljs](.joyride/scripts/workspace_activate.cljs)

Scripts not in this project, featured in the demo:

* [Awesome Copilot script](https://pez.github.io/awesome-copilot-index/awesome-copilot-script)
* [Git Fuzzy Search Menu](https://github.com/BetterThanTomorrow/joyride/tree/master/examples#fuzzy-git-history-search-menu)

## Get Started with Joyride

1. Install the Joyride extension from VS Code's **Extensions** pane
2. From the Command Palette: <kbd>Joyride: Open User Joyride Directory in New Window</kbd>
  * This opens your Joyride User scripts project
  * The README automatically opens in preview.
3. Follow instructions in the README
   * And also ask Copilot about how to use Joyride (Sonnet 4 is best, to my experience)

(The User project README will recommend to also install the Calva extension. You really should.)

Also recommended: Cloning and opening this project in VS Code and follow along with the demo video. See [how to use this project](#how-to-use-this-project)


## Joyride

* [@ VS Code Marketplace](https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.joyride)
* [github.com/BetterThanTomorrow/joyride](https://github.com/BetterThanTomorrow/joyride)
  * [Joyride API](https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md)
  * [Examples](https://github.com/BetterThanTomorrow/joyride/tree/master/examples)
* [Playlist on CalvaTV (YouTube)](https://www.youtube.com/playlist?list=PLPb7X_9OOo7otHhDdSWnh_G9B531whMRx)
* Slack: Join [Clojurians slack](http://clojurians.net), and `#joyride`

Joyride was created by Peter Strömberg and Michiel Borkent (a.k.a. PEZ and Borkdude). We are two tool smiths embedded in the [Clojure](https://clojure.org/) community.

Peter mainly serves VS Code users, with [Calva](https://calva.io), and Joyride, for which I am the maintainer.
* Github: [@PEZ](https://github.com/PEZ)
* X: [@pappapez](https://twitter.com/pappapez)
* LinkedIn: [Peter Strömberg](https://www.linkedin.com/in/cospaia)

Michiel makes foundational tools for the Clojure community at large. Like [Babashka](https://babashka.org). And [SCI](https://github.com/babashka/sci), the Small Clojure Interpreter, which powers both Babashka and Joyride.
* Github: [@borkdude](https://github.com/borkdude)
* X: [@borkdude](https://twitter.com/borkdude)
* LinkedIn: [Michiel Borkent](https://www.linkedin.com/in/michielborkent)

## Clojure

* [Get Started with Clojure using VS Code](https://calva.io/get-started-with-clojure/)
* [clojure.org](https://clojure.org/)
* [clojurescript.org](https://clojurescript.org/)
