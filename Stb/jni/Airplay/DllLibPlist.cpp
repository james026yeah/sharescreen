#include <dlfcn.h>
#include <stdio.h>
#include "DllLibPlist.h"
#define LIBPLIST_FILE "/data/data/archermind.ashare/lib/libplist.so"

DllLibPlist::DllLibPlist() :
	_plist_new_dict(NULL),
	_plist_dict_get_item(NULL),
	_plist_dict_get_size(NULL),
	_plist_from_bin(NULL),
	_plist_get_string_val(NULL),
	_plist_get_real_val(NULL),
	_plist_free(NULL),
	m_handler(NULL)
#ifdef TARGET_WINDOWS
	,
	_plist_free_string_val(NULL)
#endif
{
}

DllLibPlist::~DllLibPlist()
{
	if (IsLoaded())
		Unload();
}

bool DllLibPlist::Load(void)
{
	const char *error;

	if (IsLoaded())
		return true;

	m_handler = dlopen(LIBPLIST_FILE, RTLD_LAZY);
	if (!m_handler) {
		fprintf(stderr, "%s\n", dlerror());
		return false;
	}

	*(void **)&_plist_new_dict = dlsym(m_handler, "plist_new_dict");
	if ((error = dlerror()) != NULL) {
		fprintf(stderr, "%s\n", error);
		Unload();
		return false;
	}

	*(void **)&_plist_dict_get_item = dlsym(m_handler, "plist_dict_get_item");
	if ((error = dlerror()) != NULL) {
		fprintf(stderr, "%s\n", error);
		Unload();
		return false;
	}

	*(void **)&_plist_dict_get_size = dlsym(m_handler, "plist_dict_get_size");
	if ((error = dlerror()) != NULL) {
		fprintf(stderr, "%s\n", error);
		Unload();
		return false;
	}

	*(void **)&_plist_from_bin = dlsym(m_handler, "plist_from_bin");
	if ((error = dlerror()) != NULL) {
		fprintf(stderr, "%s\n", error);
		Unload();
		return false;
	}

	*(void **)&_plist_get_string_val = dlsym(m_handler, "plist_get_string_val");
	if ((error = dlerror()) != NULL) {
		fprintf(stderr, "%s\n", error);
		Unload();
		return false;
	}

	*(void **)&_plist_get_real_val = dlsym(m_handler, "plist_get_real_val");
	if ((error = dlerror()) != NULL) {
		fprintf(stderr, "%s\n", error);
		Unload();
		return false;
	}

	*(void **)&_plist_free = dlsym(m_handler, "plist_free");
	if ((error = dlerror()) != NULL) {
		fprintf(stderr, "%s\n", error);
		Unload();
		return false;
	}

	return true;
}

void DllLibPlist::Unload(void)
{
	if (IsLoaded())
		dlclose(m_handler);

	*(void **)&_plist_new_dict = NULL;
	*(void **)&_plist_dict_get_item = NULL;
	*(void **)&_plist_dict_get_size = NULL;
	*(void **)&_plist_from_bin = NULL;
	*(void **)&_plist_get_string_val = NULL;
	*(void **)&_plist_get_real_val = NULL;
	*(void **)&_plist_free = NULL;
	m_handler = NULL;
	return;
}

bool DllLibPlist::IsLoaded(void)
{
	if (m_handler)
		return true;
	return false;
}

bool DllLibPlist::EnableDelayedUnload(bool onOff)
{
	return true;
}

plist_t DllLibPlist::plist_new_dict(void)
{
	if (IsLoaded())
		return _plist_new_dict();
	return NULL;
}

plist_t DllLibPlist::plist_dict_get_item(plist_t node, const char* key)
{
	if (IsLoaded())
		return _plist_dict_get_item(node, key);
	return NULL;
}

uint32_t DllLibPlist::plist_dict_get_size(plist_t node)
{
	if (IsLoaded())
		return _plist_dict_get_size(node);
	return 0;
}

void DllLibPlist::plist_from_bin(const char *plist_bin, uint32_t length, plist_t * plist)
{
	if (IsLoaded())
		_plist_from_bin(plist_bin, length, plist);
	return;
}

void DllLibPlist::plist_get_string_val(plist_t node, char **val)
{
	if (IsLoaded())
		_plist_get_string_val(node, val);
	return;
}

void DllLibPlist::plist_get_real_val(plist_t node, double *val)
{
	if (IsLoaded())
		_plist_get_real_val(node, val);
	return;
}

void DllLibPlist::plist_free(plist_t plist)
{
	if (IsLoaded())
		_plist_free(plist);
	return;
}

#ifdef TARGET_WINDOWS
void DllLibPlist::plist_free_string_val (char *val)
{
	return;
}
#endif
