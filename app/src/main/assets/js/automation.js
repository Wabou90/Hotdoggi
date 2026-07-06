(function () {
  'use strict';

  if (window.__flowAutomationInjected) return;
  window.__flowAutomationInjected = true;

  var SELECTORS = {
    createProjectButton: 'button:has(i:contains("add_2")):first()',
    disableAgentModeButton:
      'div:has(div[data-scroll-state="START"]) button[aria-pressed="true"], button:has(i:contains("close")):has(span:contains("prompt")), div:has(i:contains("edit_square")) > button:has(i:contains("close"))',
    enableAgentModeButton:
      'div:has(div[data-scroll-state="START"]) button[aria-pressed="false"], button:has(i:contains("expand_content"))',
    configureUIModeButton: 'button:has(i:contains("settings_2"))',
    selectVideoMode: 'div[data-state="open"] div[role="tablist"]:eq(0) button:eq(1)',
    selectImageMode: 'div[data-state="open"] div[role="tablist"]:eq(0) button:eq(0)',
    textToVideoModeOption: 'div[data-state="open"] div[role="tablist"]:eq(1) button:eq(1)',
    imageToVideoModeOption: 'div[data-state="open"] div[role="tablist"]:eq(1) button:eq(0)',
    promptTextarea: 'div[role="textbox"]',
    submitButton: 'button:has(i:contains("arrow_forward"))',
    stopButton: 'button:has(i:contains("stop"))',
    downloadDoneButton: 'button:has(i:contains("check")), header button:last(), button:has(span:contains("Done"))',
    outputItems: 'div > div > div[data-tile-id]:has(div)',
    tileByIdTemplate: 'div[data-tile-id="{tileId}"]:has(div)',
    configButton: 'button:has(i:contains("crop")), button:has(i:contains("tune"))',
    outputCountTemplate: 'div[data-state="open"] div[role="tablist"] button:contains("{outputCount}")',
    aspectRatioTemplate: 'div[data-state="open"] div[role="tablist"] button:has(i:contains("{aspectRatio}"))',
    modelSelectButton: 'div[data-state="open"] button:has(i:contains("arrow_drop_down"))',
    modelTemplate: 'div[role="menu"] button:has(span:contains("{model}"))',
    videoLengthTemplate: 'div[data-orientation="horizontal"] > div > button:contains("{videoLength}")',
    quality1080Option: 'button:has(span:contains("1080p"))',
    quality2KOption: 'button:has(span:contains("2K"))',
    quality4KOption: 'button:has(span:contains("4K"))',
    moreOptionsButtonInHoverTile: 'button:has(i:contains("more_vert"))',
    downloadButtonInHoverTile: 'div[aria-haspopup="menu"] i:contains("download")',
    tileOnQueue: 'i:contains("movie"), div[style*="brightness(1)"]',
  };

  var running = false;
  var currentGroupId = '';
  var currentPrompts = [];
  var config = {};
  var state = {
    folderName: '',
    prefix: '',
    autoChangeFileName: true,
    mode: 'video',
    videoMode: 'TEXT_TO_VIDEO',
    imageMode: 'CREATE_NEW',
    outputCount: 1,
    aspectRatio: '16:9',
    quality: '1080p',
    duration: '8s',
    model: '',
    maxConcurrent: 3,
  };

  function log(level, msg) {
    try {
      AndroidBridge.postMessage(
        JSON.stringify({ type: 'ACTION_LOG', data: { level: level, message: msg } })
      );
    } catch (e) {}
  }

  function $(sel, ctx) {
    ctx = ctx || document;
    var s = sel.trim();
    if (s.indexOf(':contains(') !== -1) return querySelectorContains(s, ctx);
    if (s.indexOf(':eq(') !== -1) return querySelectorEq(s, ctx);
    try {
      return ctx.querySelector(s);
    } catch (e) {
      return null;
    }
  }

  function $$(sel, ctx) {
    ctx = ctx || document;
    var s = sel.trim();
    if (s.indexOf(':contains(') !== -1) return querySelectorAllContains(s, ctx);
    if (s.indexOf(':eq(') !== -1) return querySelectorEqAll(s, ctx);
    try {
      return Array.from(ctx.querySelectorAll(s));
    } catch (e) {
      return [];
    }
  }

  function querySelectorContains(sel, ctx) {
    var parts = splitSelector(sel);
    var current = [ctx];
    for (var i = 0; i < parts.length; i++) {
      current = filterContains(parts[i], current);
    }
    return current.length > 0 ? current[0] : null;
  }

  function querySelectorAllContains(sel, ctx) {
    var parts = splitSelector(sel);
    var current = [ctx];
    for (var i = 0; i < parts.length; i++) {
      current = filterContains(parts[i], current);
    }
    return current;
  }

  function querySelectorEq(sel, ctx) {
    var match = sel.match(/^(.+?):eq\((\d+)\)$/);
    if (!match) return ctx.querySelector(sel);
    var base = match[1].trim();
    var index = parseInt(match[2]);
    var els = Array.from(ctx.querySelectorAll(base));
    return els[index] || null;
  }

  function querySelectorEqAll(sel, ctx) {
    var match = sel.match(/^(.+?):eq\((\d+)\)$/);
    if (!match) return Array.from(ctx.querySelectorAll(sel));
    var base = match[1].trim();
    var index = parseInt(match[2]);
    var els = Array.from(ctx.querySelectorAll(base));
    return els[index] ? [els[index]] : [];
  }

  function splitSelector(sel) {
    var parts = [];
    var depth = 0;
    var current = '';
    for (var i = 0; i < sel.length; i++) {
      var ch = sel[i];
      if (ch === '(') depth++;
      else if (ch === ')') depth--;
      if (ch === ',' && depth === 0) {
        parts.push(current.trim());
        current = '';
      } else {
        current += ch;
      }
    }
    if (current.trim()) parts.push(current.trim());
    return parts;
  }

  function filterContains(part, contexts) {
    var results = [];
    var containsMatch = part.match(/:contains\("([^"]*)"\)/);
    var hasMatch = part.match(/:has\(([^)]+)\)/);
    var tagMatch = part.match(/^(\w+)/);
    var classMatch = part.match(/\.([\w-]+)/g);
    var attrMatch = part.match(/\[([^\]]+)\]/g);
    var pseudoMatch = part.match(/:first|:last/g);

    var tag = tagMatch ? tagMatch[1] : '*';
    var classes = classMatch
      ? classMatch.map(function (c) { return c.substring(1); })
      : [];
    var attrs = attrMatch
      ? attrMatch.map(function (a) { return a.slice(1, -1); })
      : [];

    for (var c = 0; c < contexts.length; c++) {
      var ctx = contexts[c];
      var all = Array.from(ctx.querySelectorAll(tag));
      for (var i = 0; i < all.length; i++) {
        var el = all[i];
        var match = true;

        if (containsMatch) {
          var searchText = containsMatch[1];
          if (el.textContent.indexOf(searchText) === -1) match = false;
        }

        if (match && classes.length) {
          for (var cl = 0; cl < classes.length; cl++) {
            if (!el.classList.contains(classes[cl])) {
              match = false;
              break;
            }
          }
        }

        if (match && attrs.length) {
          for (var a = 0; a < attrs.length; a++) {
            var kv = attrs[a].split('=');
            var attrName = kv[0].trim();
            var attrVal = kv[1] ? kv[1].replace(/"/g, '').trim() : null;
            if (attrVal) {
              if (el.getAttribute(attrName) !== attrVal) match = false;
            } else {
              if (!el.hasAttribute(attrName)) match = false;
            }
          }
        }

        if (match && pseudoMatch) {
          var parent = el.parentElement;
          if (parent) {
            var siblings = Array.from(parent.children).filter(function (s) {
              return s.matches(tag);
            });
            if (pseudoMatch.indexOf(':first') !== -1 && siblings.indexOf(el) !== 0) match = false;
            if (pseudoMatch.indexOf(':last') !== -1 && siblings.indexOf(el) !== siblings.length - 1)
              match = false;
          }
        }

        if (match) results.push(el);
      }
    }

    if (hasMatch) {
      var hasSel = hasMatch[1].trim();
      results = results.filter(function (el) {
        return querySelectorContains(hasSel, el);
      });
    }

    return results;
  }

  function sleep(ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  }

  function waitForElement(sel, timeout) {
    timeout = timeout || 10000;
    var start = Date.now();
    return new Promise(function (resolve, reject) {
      function check() {
        var el = $(sel);
        if (el) return resolve(el);
        if (Date.now() - start > timeout) return reject(new Error('Timeout waiting for: ' + sel));
        requestAnimationFrame(check);
      }
      check();
    });
  }

  function waitForElementRemoved(sel, timeout) {
    timeout = timeout || 10000;
    var start = Date.now();
    return new Promise(function (resolve, reject) {
      function check() {
        var el = $(sel);
        if (!el) return resolve();
        if (Date.now() - start > timeout) return reject(new Error('Timeout waiting for removal: ' + sel));
        requestAnimationFrame(check);
      }
      check();
    });
  }

  function click(el) {
    if (!el) return false;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    el.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
    el.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    return true;
  }

  function typeText(el, text) {
    if (!el) return;
    el.focus();
    el.textContent = '';
    document.execCommand('insertText', false, text);
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function resolveSelector(template, vars) {
    var s = template;
    for (var key in vars) {
      s = s.replace(new RegExp('\\{' + key + '\\}', 'g'), vars[key]);
    }
    return s;
  }

  function loadConfig() {
    try {
      var json = AndroidBridge.getConfig();
      if (json) {
        var parsed = JSON.parse(json);
        for (var key in parsed) {
          if (parsed[key] === 'true') parsed[key] = true;
          if (parsed[key] === 'false') parsed[key] = false;
          if (key === 'outputCount' || key === 'maxConcurrent') parsed[key] = parseInt(parsed[key], 10);
        }
        state.folderName = parsed.folderName || '';
        state.prefix = parsed.prefix || '';
        state.autoChangeFileName = parsed.autoChangeFileName !== false;
        state.mode = parsed.mode || 'video';
        state.videoMode = parsed.videoMode || 'TEXT_TO_VIDEO';
        state.imageMode = parsed.imageMode || 'CREATE_NEW';
        state.outputCount = parsed.outputCount || 1;
        state.aspectRatio = parsed.aspectRatio || '16:9';
        state.quality = parsed.quality || '1080p';
        state.duration = parsed.duration || '8s';
        state.model = parsed.model || '';
        state.maxConcurrent = parsed.maxConcurrent || 3;
      }
    } catch (e) {
      log('warn', 'Config parse error: ' + e.message);
    }
  }

  function loadPrompts() {
    try {
      var json = AndroidBridge.getPrompts();
      if (json) {
        var parsed = JSON.parse(json);
        currentGroupId = parsed.groupId || '';
        currentPrompts = parsed.prompts || [];
      }
    } catch (e) {
      log('warn', 'Prompts parse error: ' + e.message);
    }
  }

  async function ensureCreateProject() {
    var btn = $(SELECTORS.createProjectButton);
    if (btn) {
      log('info', 'Creating new project...');
      click(btn);
      await sleep(2000);
      await waitForElement(SELECTORS.promptTextarea, 8000);
    }
  }

  async function disableAgentMode() {
    var btn = $(SELECTORS.disableAgentModeButton);
    if (btn) {
      log('info', 'Disabling agent mode...');
      click(btn);
      await sleep(1000);
    }
  }

  async function configureVideoMode() {
    var configBtn = $(SELECTORS.configButton);
    if (!configBtn) {
      log('warn', 'Config button not found');
      return;
    }
    click(configBtn);
    await sleep(1000);

    var videoTab = $(SELECTORS.selectVideoMode);
    if (videoTab) {
      click(videoTab);
      await sleep(500);
    }

    if (state.videoMode === 'TEXT_TO_VIDEO') {
      var ttv = $(SELECTORS.textToVideoModeOption);
      if (ttv) click(ttv);
    } else if (state.videoMode === 'IMAGE_TO_VIDEO') {
      var itv = $(SELECTORS.imageToVideoModeOption);
      if (itv) click(itv);
    }

    await sleep(500);

    var countSel = resolveSelector(SELECTORS.outputCountTemplate, { outputCount: String(state.outputCount) });
    var countEl = $(countSel);
    if (countEl) click(countEl);

    await sleep(300);

    var ratioSel = resolveSelector(SELECTORS.aspectRatioTemplate, {
      aspectRatio: state.aspectRatio.replace(':', ''),
    });
    var ratioEl = $(ratioSel);
    if (ratioEl) click(ratioEl);

    await sleep(300);

    if (state.model) {
      var modelBtn = $(SELECTORS.modelSelectButton);
      if (modelBtn) {
        click(modelBtn);
        await sleep(500);
        var modelOpt = $(resolveSelector(SELECTORS.modelTemplate, { model: state.model }));
        if (modelOpt) click(modelOpt);
        await sleep(300);
      }
    }

    if (state.duration) {
      var durSel = resolveSelector(SELECTORS.videoLengthTemplate, { videoLength: state.duration });
      var durEl = $(durSel);
      if (durEl) click(durEl);
    }

    var closeBtn = $(SELECTORS.configureUIModeButton);
    if (closeBtn) click(closeBtn);
    await sleep(500);
  }

  async function configureImageMode() {
    var configBtn = $(SELECTORS.configButton);
    if (!configBtn) return;
    click(configBtn);
    await sleep(1000);

    var imgTab = $(SELECTORS.selectImageMode);
    if (imgTab) click(imgTab);

    await sleep(500);

    var countSel = resolveSelector(SELECTORS.outputCountTemplate, { outputCount: String(state.outputCount) });
    var countEl = $(countSel);
    if (countEl) click(countEl);

    var closeBtn = $(SELECTORS.configureUIModeButton);
    if (closeBtn) click(closeBtn);
    await sleep(500);
  }

  async function configureMode() {
    var uiBtn = $(SELECTORS.configureUIModeButton);
    if (uiBtn) {
      click(uiBtn);
      await sleep(500);
    }

    if (state.mode === 'video') {
      await configureVideoMode();
    } else {
      await configureImageMode();
    }
  }

  async function fillPrompt(text) {
    var textarea = $(SELECTORS.promptTextarea);
    if (!textarea) {
      log('error', 'Prompt textarea not found');
      return false;
    }
    typeText(textarea, text);
    await sleep(500);
    return true;
  }

  async function submitPrompt(groupId, promptIndex, promptText) {
    log('info', 'Submitting prompt #' + (promptIndex + 1) + ': ' + promptText.substring(0, 60));

    var submitBtn = $(SELECTORS.submitButton);
    if (!submitBtn) {
      log('error', 'Submit button not found');
      reportProgress(groupId, promptIndex, promptText, 'failed', 0);
      return false;
    }

    click(submitBtn);
    await sleep(3000);

    log('info', 'Waiting for generation...');
    var maxWait = 180;
    for (var i = 0; i < maxWait; i++) {
      if (!running) {
        log('warn', 'Automation stopped during generation');
        return false;
      }

      var stopBtn = $(SELECTORS.stopButton);
      var doneBtn = $(SELECTORS.downloadDoneButton);

      if (doneBtn) {
        log('info', 'Generation completed for #' + (promptIndex + 1));
        reportProgress(groupId, promptIndex, promptText, 'completed', 100);
        await sleep(1000);
        return true;
      }

      if (!stopBtn) {
        await sleep(2000);
        var doneBtn2 = $(SELECTORS.downloadDoneButton);
        if (doneBtn2) {
          log('info', 'Generation completed for #' + (promptIndex + 1));
          reportProgress(groupId, promptIndex, promptText, 'completed', 100);
          await sleep(1000);
          return true;
        }
        log('error', 'Generation stopped unexpectedly for #' + (promptIndex + 1));
        reportProgress(groupId, promptIndex, promptText, 'failed', 0);
        return false;
      }

      reportProgress(groupId, promptIndex, promptText, 'generating', Math.min(95, Math.floor((i / maxWait) * 100)));
      await sleep(2000);
    }

    log('error', 'Generation timeout for #' + (promptIndex + 1));
    reportProgress(groupId, promptIndex, promptText, 'failed', 0);
    return false;
  }

  async function downloadGenerated(groupId, promptIndex, promptText) {
    await sleep(2000);

    var downloadBtn = $(SELECTORS.downloadButtonInHoverTile);
    if (downloadBtn) {
      log('info', 'Downloading from tile menu...');
      click(downloadBtn);
      await sleep(1500);

      var qSel = state.quality === '2K'
        ? SELECTORS.quality2KOption
        : state.quality === '4K'
          ? SELECTORS.quality4KOption
          : SELECTORS.quality1080Option;
      var qEl = $(qSel);
      if (qEl) click(qEl);
      await sleep(3000);
    }

    var doneBtn = $(SELECTORS.downloadDoneButton);
    if (doneBtn) {
      click(doneBtn);
      await sleep(1000);
    }

    reportProgress(groupId, promptIndex, promptText, 'completed', 100);
  }

  function reportProgress(groupId, promptIndex, promptText, status, percentage) {
    try {
      AndroidBridge.postMessage(
        JSON.stringify({
          type: 'PROMPT_GROUP_STATUS',
          data: {
            groupId: groupId,
            promptIndex: promptIndex,
            prompt: promptText,
            status: status,
            percentage: percentage,
          },
        })
      );
    } catch (e) {}
  }

  async function runGroup(groupId, prompts) {
    log('info', 'Starting group with ' + prompts.length + ' prompts');
    running = true;

    await ensureCreateProject();
    await disableAgentMode();
    await sleep(500);
    await configureMode();
    await sleep(500);

    for (var i = 0; i < prompts.length; i++) {
      if (!running) {
        log('warn', 'Automation stopped mid-group');
        break;
      }

      var promptText = prompts[i];
      var filled = await fillPrompt(promptText);
      if (!filled) {
        reportProgress(groupId, i, promptText, 'failed', 0);
        continue;
      }

      var submitted = await submitPrompt(groupId, i, promptText);
      if (!submitted) continue;

      await downloadGenerated(groupId, i, promptText);
      await sleep(2000);
    }

    log('info', 'Group completed');
    running = false;
    try {
      AndroidBridge.postMessage(
        JSON.stringify({
          type: 'AUTO_FILL_FLOW',
          data: { groupId: groupId, status: 'completed' },
        })
      );
    } catch (e) {}
  }

  function startAutomation() {
    if (running) {
      log('warn', 'Already running');
      return;
    }
    loadConfig();
    loadPrompts();
    if (!currentPrompts.length) {
      log('error', 'No prompts loaded');
      return;
    }
    log('info', 'Starting automation with ' + currentPrompts.length + ' prompts');
    runGroup(currentGroupId, currentPrompts).catch(function (err) {
      log('error', 'Automation error: ' + err.message);
      running = false;
    });
  }

  function stopAutomation() {
    running = false;
    log('info', 'Stopping automation...');
    var stopBtn = $(SELECTORS.stopButton);
    if (stopBtn) click(stopBtn);
  }

  function clearCache() {
    try {
      AndroidBridge.postMessage(
        JSON.stringify({
          type: 'CS',
          tabId: null,
        })
      );
      log('info', 'Cache clear requested');
    } catch (e) {}
  }

  function setZoom(factor) {
    document.body.style.zoom = factor;
  }

  window.__flowAutomation = {
    start: startAutomation,
    stop: stopAutomation,
    clearCache: clearCache,
    setZoom: setZoom,
    getState: function () { return state; },
    getSelectors: function () { return SELECTORS; },
  };

  log('info', 'Flow Automation injected');
})();
