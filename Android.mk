#
# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

ifeq ($(TARGET_BUILD_APPS),)
support_library_root_dir := frameworks/support
else
support_library_root_dir := prebuilts/sdk/current/support
endif

LOCAL_STATIC_JAVA_LIBRARIES := \
    guava \
    android-common \
    android-support-v7-appcompat \
    android-support-v4

LOCAL_JAVA_LIBRARIES += org.apache.http.legacy

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \
    $(call all-logtags-files-under, src)

LOCAL_PACKAGE_NAME := SprdQuickSearchBox

LOCAL_OVERRIDES_PACKAGES := QuickSearchBox

LOCAL_CERTIFICATE := platform

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    $(support_library_root_dir)/v7/appcompat/res

LOCAL_USE_AAPT2 := true

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.appcompat

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Also build our test apk
include $(call all-makefiles-under,$(LOCAL_PATH))
