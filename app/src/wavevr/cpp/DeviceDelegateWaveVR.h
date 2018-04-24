#ifndef DEVICE_DELEGATE_WAVE_VR_DOT_H
#define DEVICE_DELEGATE_WAVE_VR_DOT_H

#include "vrb/Forward.h"
#include "vrb/MacroUtils.h"
#include "DeviceDelegate.h"
#include <memory>

namespace crow {

class DeviceDelegateWaveVR;
typedef std::shared_ptr<DeviceDelegateWaveVR> DeviceDelegateWaveVRPtr;

class DeviceDelegateWaveVR : public DeviceDelegate {
public:
  static DeviceDelegateWaveVRPtr Create(vrb::ContextWeak aContext);
  // DeviceDelegate interface
  GestureDelegateConstPtr GetGestureDelegate() override;
  vrb::CameraPtr GetCamera(const CameraEnum aWhich) override;
  const vrb::Matrix& GetHeadTransform() const override;
  void SetClearColor(const vrb::Color& aColor) override;
  void SetClipPlanes(const float aNear, const float aFar) override;
  int32_t GetControllerCount() const override;
  const std::string GetControllerModelName(const int32_t aWhichContorller) const override;
  void ProcessEvents() override;
  const vrb::Matrix& GetControllerTransform(const int32_t aWhichController) override;
  bool GetControllerButtonState(const int32_t aWhichController, const int32_t aWhichButton, bool& aChangedState) override;
  bool GetControllerScrolled(const int32_t aWhichController, float& aScrollX, float& aScrollY) override;
  bool IsControllerUsingHeadTracking(const int32_t aWhichController) const override { return false; };
  void StartFrame() override;
  void BindEye(const CameraEnum aWhich) override;
  void EndFrame() override;
  // DeviceDelegateWaveVR interface
  bool IsRunning();
protected:
  struct State;
  DeviceDelegateWaveVR(State& aState);
  virtual ~DeviceDelegateWaveVR();
private:
  State& m;
  VRB_NO_DEFAULTS(DeviceDelegateWaveVR)
};

} // namespace crow
#endif // DEVICE_DELEGATE_WAVE_VR_DOT_H
