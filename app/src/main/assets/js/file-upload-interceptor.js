(function () {
  'use strict';

  if (window.__flowFileUploadInjected) return;
  window.__flowFileUploadInjected = true;

  var capturedInput = null;

  var originalClick = HTMLInputElement.prototype.click;
  HTMLInputElement.prototype.click = function () {
    if (this.type !== 'file') {
      return originalClick.apply(this, arguments);
    }
    if (document.documentElement.getAttribute('data-veo-active') !== 'true') {
      return originalClick.apply(this, arguments);
    }
    capturedInput = this;
  };

  document.addEventListener('VEO_UPLOAD_FILE_DATA', function (e) {
    var detail = e.detail;
    if (!detail || !capturedInput) return;

    try {
      var base64 = detail.base64 || '';
      var filename = detail.filename || 'upload';
      var mimeType = detail.mimeType || 'image/png';

      if (base64.indexOf(',') !== -1) {
        base64 = base64.split(',')[1];
      }

      var binaryStr = atob(base64);
      var bytes = new Uint8Array(binaryStr.length);
      for (var i = 0; i < binaryStr.length; i++) {
        bytes[i] = binaryStr.charCodeAt(i);
      }

      var blob = new Blob([bytes], { type: mimeType });
      var file = new File([blob], filename, { type: mimeType });

      var dt = new DataTransfer();
      dt.items.add(file);
      capturedInput.files = dt.files;
      capturedInput.dispatchEvent(new Event('change', { bubbles: true }));
    } catch (err) {
      console.error('File upload error:', err);
    } finally {
      capturedInput = null;
    }
  });

  window.__uploadFile = function (base64, filename, mimeType) {
    document.dispatchEvent(
      new CustomEvent('VEO_UPLOAD_FILE_DATA', {
        detail: { base64: base64, filename: filename, mimeType: mimeType },
      })
    );
  };
})();
